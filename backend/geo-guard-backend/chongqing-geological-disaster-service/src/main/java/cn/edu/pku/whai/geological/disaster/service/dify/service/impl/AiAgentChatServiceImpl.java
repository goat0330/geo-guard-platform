/**
 * @author kongweiguang
 */
package cn.edu.pku.whai.geological.disaster.service.dify.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.domain.dto.OssDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.OssService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailBo;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiAgentChatService;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailService;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryService;
import cn.edu.pku.whai.geological.disaster.service.mcp.manager.UserConfirmationManager;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.enums.FileTransferMethod;
import io.github.imfangs.dify.client.enums.FileType;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.NodeFinishedEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.SuggestedQuestionsResponse;
import io.github.imfangs.dify.client.model.file.FileInfo;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import io.github.kongweiguang.json.Json;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Data
@Service
@RequiredArgsConstructor
public class AiAgentChatServiceImpl implements IAiAgentChatService {
    private final UserConfirmationManager userConfirmationManager;
    private final DifyAgentClient difyAgentClient;
    private final OssService ossService;
    private final IAiChatHistoryService aiChatHistoryService;
    private final IAiChatHistoryDetailService aiChatHistoryDetailService;

    @Override
    public void chat(SseEmitter emitter, AiChatBo bo) {
        String tokenValue = StpUtil.getTokenValue();
        Long userId = LoginHelper.getUserId();
        UserConfirmationManager.tokenEmitter.put(tokenValue, emitter);
        DifyChatflowClient agent = difyAgentClient.getChatAgent();

        ChatMessage chatMessage = BeanUtil.copyProperties(bo, ChatMessage.class);
        chatMessage.setUser(LoginHelper.getUsername());
        Map<String, Object> inputs = chatMessage.getInputs();
        inputs.put("token", tokenValue);
        chatMessage.setInputs(inputs);

        AiChatHistoryDetailBo chatHistoryDetail = new AiChatHistoryDetailBo();
        chatHistoryDetail.setUserId(userId);
        chatHistoryDetail.setInputs(inputs);
        chatHistoryDetail.setFiles(bo.getFiles());
        chatHistoryDetail.setQuery(bo.getQuery());
        chatHistoryDetail.setCreateDate(new Date());

        AtomicBoolean requestSaved = new AtomicBoolean(false);
        try {
            chatMessage.setFiles(uploadOssFiles(chatMessage.getFiles(), agent, chatMessage.getUser()));
            agent.sendChatMessageStream(chatMessage, new ChatflowStreamCallback() {
                /** 外部错误事件可能含用户内容；仅记录失败类型，保持原连接清理语义。 */
                @Override
                public void onError(ErrorEvent event) {
                    log.error("sendChatMessageStream received an error event");
                    UserConfirmationManager.tokenEmitter.remove(tokenValue);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("sendChatMessageStream onException: -> ", throwable);
                    UserConfirmationManager.tokenEmitter.remove(tokenValue);
                }

                @Override
                public void onMessage(MessageEvent event) {
                    saveChatRequestIfNeeded(bo, userId, chatHistoryDetail, event, requestSaved);
                    sendEmitter(emitter, event);
                }


                @Override
                public void onNodeFinished(NodeFinishedEvent event) {
                    NodeFinishedEvent.NodeFinishedData data = event.getData();
                    String nodeType = data.getNodeType();
                    if ("knowledge-retrieval".equals(nodeType)) {
                        sendEmitter(emitter, event);
                        chatHistoryDetail.setMessageMetadata(BeanUtil.beanToMap(event));
                    }

                    if ("http-request".equals(nodeType) && "知识库".equals(data.getTitle())) {
                        sendEmitter(emitter, event);
                        chatHistoryDetail.setMessageMetadata(BeanUtil.beanToMap(event));
                    }

                    if ("Knowledge Showcase".equals(data.getTitle())) {
                        sendEmitter(emitter, event);
                    }

                }

                @Override
                public void onWorkflowFinished(WorkflowFinishedEvent event) {
                    try {
                        if (!requestSaved.get()) {
                            return;
                        }
                        Map<String, Object> data = event.getData().getOutputs();
                        chatHistoryDetail.setAnswer(Convert.toStr(data.get("answer")));
                        aiChatHistoryDetailService.updateByBo(chatHistoryDetail);
                    } catch (Exception e) {
                        log.error("保存结果报错 ->", e);
                    } finally {
                        sendEmitter(emitter, event);
                        emitter.complete();
                        UserConfirmationManager.tokenEmitter.remove(tokenValue);
                    }

                }
            });
        } catch (Exception e) {
            log.error("发送sse消息失败 -> ", e);
            emitter.complete();
            UserConfirmationManager.tokenEmitter.remove(tokenValue);
        }

    }

    private void saveChatRequestIfNeeded(AiChatBo bo, Long userId, AiChatHistoryDetailBo chatHistoryDetail,
                                         MessageEvent event, AtomicBoolean requestSaved) {
        if (requestSaved.get() || event == null || StringUtils.isBlank(event.getMessageId())) {
            return;
        }
        if (requestSaved.compareAndSet(false, true)) {
            chatHistoryDetail.setId(event.getMessageId());
            chatHistoryDetail.setHistoryId(event.getConversationId());
            if (StringUtils.isBlank(bo.getConversationId())) {
                insertChatHistory(bo, event.getConversationId(), userId);
            }
            aiChatHistoryDetailService.insertByBo(chatHistoryDetail);
        }
    }

    private List<FileInfo> uploadOssFiles(List<FileInfo> files, DifyChatflowClient agent, String user) {
        if (CollUtil.isEmpty(files)) {
            return files;
        }
        List<Long> ossIds = files.stream()
            .filter(Objects::nonNull)
            .filter(file -> FileTransferMethod.LOCAL_FILE == file.getTransferMethod())
            .map(FileInfo::getUploadFileId)
            .map(uploadFileId -> Convert.toLong(uploadFileId, null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(ossIds)) {
            return files;
        }

        Map<Long, OssDTO> ossMap = ossService.selectByIds(ossIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(OssDTO::getOssId, Function.identity(), (left, right) -> left));
        OssClient storage = OssFactory.instance();
        FileUploadRequest uploadRequest = FileUploadRequest.builder().user(user).build();
        return files.stream().map(file -> uploadSingleOssFile(file, agent, uploadRequest, storage, ossMap)).toList();
    }

    private FileInfo uploadSingleOssFile(FileInfo file, DifyChatflowClient agent, FileUploadRequest uploadRequest,
                                         OssClient storage, Map<Long, OssDTO> ossMap) {
        if (file == null || FileTransferMethod.LOCAL_FILE != file.getTransferMethod()) {
            return file;
        }
        Long ossId = Convert.toLong(file.getUploadFileId(), null);
        if (ossId == null) {
            return file;
        }
        OssDTO oss = ossMap.get(ossId);
        if (oss == null) {
            throw new ServiceException("附件不存在，ossId=" + ossId);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        storage.download(oss.getFileName(), outputStream, null);
        String originalName = StrUtil.blankToDefault(oss.getOriginalName(), oss.getFileName());
        try {
            FileUploadResponse response = agent.uploadFile(uploadRequest, new ByteArrayInputStream(outputStream.toByteArray()), originalName);
            if (response == null || StrUtil.isBlank(response.getId())) {
                throw new ServiceException("上传智能体附件失败，ossId=" + ossId);
            }
            return FileInfo.builder()
                .type(file.getType() == null ? FileType.getByFileExtension(originalName) : file.getType())
                .transferMethod(FileTransferMethod.LOCAL_FILE)
                .uploadFileId(response.getId())
                .build();
        } catch (Exception e) {
            log.error("上传智能体附件失败，ossId={}", ossId, e);
            throw new ServiceException("上传智能体附件失败，ossId=" + ossId);
        }
    }

    private void insertChatHistory(AiChatBo bo, String conversationId, Long userId) {
        AiChatHistoryBo chatHistory = new AiChatHistoryBo();
        chatHistory.setId(conversationId);
        chatHistory.setUserId(userId);
        chatHistory.setType(bo.getAgentType());
        chatHistory.setTitle(StrUtil.sub(bo.getQuery(), 0, 20));
        chatHistory.setCreateDate(new Date());
        chatHistory.setUpdateDate(new Date());
        chatHistory.setDeleteFlag(0);
        aiChatHistoryService.insertByBo(chatHistory);
    }

    public void sendEmitter(SseEmitter emitter, Object vo) {
        try {
            emitter.send(SseEmitter.event().comment("message").data(Json.toStr(vo)));
        } catch (Exception e) {
            log.error("发送sse消息失败", e);
        }
    }

    @Override
    @SneakyThrows
    public void stopChatMessage(String taskId, String user) {
        DifyChatflowClient agent = difyAgentClient.getChatAgent();
        agent.stopChatMessage(taskId, LoginHelper.getUsername());
    }


    @Override
    @SneakyThrows
    public SuggestedQuestionsResponse getSuggestedQuestions(String messageId, String user) {
        DifyChatflowClient client = difyAgentClient.getChatAgent();
        return client.getSuggestedQuestions(messageId, LoginHelper.getUsername());
    }
}
