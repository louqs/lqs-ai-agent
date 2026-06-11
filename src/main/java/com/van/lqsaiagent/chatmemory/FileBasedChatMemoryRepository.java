package com.van.lqsaiagent.chatmemory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.Assert;

/**
 * 基于 Kryo 文件持久化的对话记忆仓库实现。
 * 适用于 AI 小说生成智能体场景：
 * - 每个 conversationId 对应一个独立的小说章节/故事线
 * - 支持跨会话持久化，重启后对话历史不丢失
 * - 高性能序列化，适合长文本（小说内容）存储
 */
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {
    /**
     * 持久化文件后缀名
     */
    private static final String FILE_EXTENSION = ".kryo";
    /**
     * 默认存储根目录
     */
    private static final String DEFAULT_BASE_DIR = "chat-memory";
    /**
     * 文件存储根目录路径
     */
    private final String baseDir;
    /**
     * Kryo 序列化引擎实例
     */
    private final Kryo kryo;
    /**
     * 内存缓存：conversationId -> 消息列表
     */
    private final Map<String, List<Message>> memoryCache;

    /**
     * 使用默认目录创建仓库。
     * 默认目录为当前工作目录下的 "chat-memory" 文件夹。
     */
    public FileBasedChatMemoryRepository() {
        this(DEFAULT_BASE_DIR);
    }

    /**
     * 指定持久化目录创建仓库。
     * 若目录不存在会自动创建，若已存在则加载其中所有历史对话。
     *
     * @param baseDir 存储对话文件的根目录路径
     */
    public FileBasedChatMemoryRepository(String baseDir) {
        Assert.hasText(baseDir, "存储目录不能为空");
        this.baseDir = baseDir;
        this.kryo = createKryo();
        this.memoryCache = new ConcurrentHashMap<>();
        // 初始化目录结构并加载历史数据
        initializeDirectory();
        loadAllConversations();
    }

    /**
     * 配置并创建 Kryo 序列化引擎。
     * 注册 Spring AI 的消息类型，确保复杂对象能正确序列化与反序列化。
     *
     * @return 配置好的 Kryo 实例
     */
    private Kryo createKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        // 注册 Spring AI 消息类型，使用 Kryo 默认的 FieldSerializer（无需实现 Serializable）
        kryo.register(UserMessage.class);
        kryo.register(AssistantMessage.class);
        kryo.register(SystemMessage.class);
        kryo.register(ArrayList.class);
        kryo.register(Message.class);

        return kryo;
    }

    /**
     * 初始化存储目录。
     * 检查目录是否存在，不存在则创建；同时验证目录可写权限。
     */
    private void initializeDirectory() {
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Assert.isTrue(dir.isDirectory() && dir.canWrite(),
                "存储目录必须是可写的有效目录: " + baseDir);
    }

    /**
     * 应用启动时加载所有已持久化的对话文件到内存缓存。
     * 遍历存储目录下所有 .kryo 文件，反序列化后存入内存映射。
     */
    private void loadAllConversations() {
        File dir = new File(baseDir);
        // 过滤出所有 kryo 持久化文件
        File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));

        if (files == null) return;

        for (File file : files) {
            // 从文件名提取对话ID（去掉后缀）
            String conversationId = file.getName().replace(FILE_EXTENSION, "");
            List<Message> messages = loadFromFile(file);
            if (messages != null) {
                memoryCache.put(conversationId, messages);
            }
        }
    }

    /**
     * 查询所有对话ID列表。
     * 会同步磁盘上的最新状态，确保返回完整的对话列表。
     *
     * @return 所有已存储的对话ID列表
     */
    @Override
    public List<String> findConversationIds() {
        // 同步文件系统状态，处理可能的外部文件变更
        syncFromDisk();
        return new ArrayList<>(memoryCache.keySet());
    }

    /**
     * 根据对话ID查询对应的消息列表。
     * 优先从内存缓存读取，缓存未命中则从磁盘文件加载。
     *
     * @param conversationId 对话唯一标识
     * @return 该对话的消息列表；若不存在则返回空列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "对话ID不能为空");

        // 先查内存缓存
        List<Message> cached = memoryCache.get(conversationId);
        if (cached != null) {
            // 返回副本，防止外部修改影响缓存
            return new ArrayList<>(cached);
        }

        // 缓存未命中，尝试从文件加载
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            List<Message> messages = loadFromFile(file);
            if (messages != null) {
                // 加载成功后回填缓存
                memoryCache.put(conversationId, messages);
                return new ArrayList<>(messages);
            }
        }
        // 对话不存在，返回不可变空列表
        return List.of();
    }

    /**
     * 保存或覆盖指定对话的完整消息列表。
     * 同时更新内存缓存和持久化文件，确保数据一致性。
     *
     * @param conversationId 对话唯一标识
     * @param messages 要保存的完整消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        // 创建副本存入缓存，防止外部引用修改影响内部状态
        List<Message> copy = new ArrayList<>(messages);
        memoryCache.put(conversationId, copy);

        // 同步写入持久化文件
        saveToFile(conversationId, copy);
    }


    /**
     * 删除指定对话及其持久化文件。
     * 同时清理内存缓存和磁盘文件。
     *
     * @param conversationId 要删除的对话ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "对话ID不能为空");

        // 移除缓存
        memoryCache.remove(conversationId);

        // 删除对应的持久化文件
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 向指定对话追加单条消息。
     * 适用于小说逐段生成的场景，比 saveAll 更高效，避免全量重写文件。
     *
     * @param conversationId 对话唯一标识（如小说章节名）
     * @param message 要追加的消息对象
     */
    public void appendMessage(String conversationId, Message message) {
        Assert.hasText(conversationId, "对话ID不能为空");
        Assert.notNull(message, "消息对象不能为空");

        // 读取现有消息列表
        List<Message> messages = findByConversationId(conversationId);
        List<Message> updated = new ArrayList<>(messages);
        // 追加新消息
        updated.add(message);

        // 全量保存更新后的列表
        saveAll(conversationId, updated);
    }

    /**
     * 清空所有对话记忆。
     * 删除内存缓存中的所有数据，以及存储目录下的所有持久化文件。
     * 此操作不可逆，请谨慎使用。
     */
    public void clearAll() {
        // 清空内存缓存
        memoryCache.clear();

        // 删除所有持久化文件
        File dir = new File(baseDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    /**
     * 获取指定对话的消息数量。
     *
     * @param conversationId 对话唯一标识
     * @return 该对话中的消息条数
     */
    public int getMessageCount(String conversationId) {
        return findByConversationId(conversationId).size();
    }

    /**
     * 强制将内存缓存中的所有数据同步写入磁盘。
     * 适用于需要确保数据立即落盘的场景（如应用关闭前）。
     */
    public void flush() {
        for (Map.Entry<String, List<Message>> entry : memoryCache.entrySet()) {
            saveToFile(entry.getKey(), entry.getValue());
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 根据对话ID生成对应的持久化文件对象。
     * 对对话ID进行安全处理，防止路径遍历攻击。
     *
     * @param conversationId 对话唯一标识
     * @return 对应该对话的持久化文件
     */
    private File getConversationFile(String conversationId) {
        // 清理非法字符，将非字母数字下划线横线的字符替换为下划线
        String safeId = conversationId.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return new File(baseDir, safeId + FILE_EXTENSION);
    }

    /**
     * 从持久化文件反序列化加载消息列表。
     *
     * @param file 要加载的 kryo 文件
     * @return 消息列表；若反序列化失败则返回 null
     */
    @SuppressWarnings("unchecked")
    private List<Message> loadFromFile(File file) {
        try (Input input = new Input(new FileInputStream(file))) {
            return (List<Message>) kryo.readClassAndObject(input);
        } catch (Exception e) {
            // 序列化失败时静默处理，返回 null 由调用方决定后续逻辑
            return null;
        }
    }

    /**
     * 将消息列表序列化并写入持久化文件。
     *
     * @param conversationId 对话唯一标识
     * @param messages 要持久化的消息列表
     * @throws RuntimeException 当文件写入失败时抛出
     */
    private void saveToFile(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeClassAndObject(output, messages);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException("保存对话失败: " + conversationId, e);
        }
    }

    /**
     * 同步磁盘上的新文件到内存缓存。
     * 用于处理外部程序或手动添加的持久化文件，确保内存状态与磁盘一致。
     */
    private void syncFromDisk() {
        File dir = new File(baseDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(FILE_EXTENSION));
        if (files == null) return;

        for (File file : files) {
            String conversationId = file.getName().replace(FILE_EXTENSION, "");
            // 只加载缓存中尚不存在的对话
            if (!memoryCache.containsKey(conversationId)) {
                List<Message> messages = loadFromFile(file);
                if (messages != null) {
                    memoryCache.put(conversationId, messages);
                }
            }
        }
    }

}
