/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.client;

import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.GetRoomTokenReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.InviteReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.QueryCallResultReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.AccessTokenResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.CallResultResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.InviteResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.props.ThreeMeetingProps;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import io.github.kongweiguang.json.Json;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MeetingApiClient {
    private final ThreeMeetingProps threeMeetingProps;

    public String getAccessToken() {
        return getAccessToken(LoginHelper.getUserIdStr(), "********").getToken();
    }

    /**
     * 签名算法实现
     * 公式：base64(HmacSHA1(MD5(access_token + data + timestamp), appkey))
     */
    public String generateSign(String accessToken, String data, long timestamp) {
        try {
            String baseString = accessToken + data + timestamp;
            String md5String = DigestUtils.md5Hex(baseString).toUpperCase();
            byte[] hmacSha1 = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, threeMeetingProps.getAppKey()).hmac(md5String);
            return URLEncoder.encode(Base64.encodeBase64String(hmacSha1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }

    /**
     * 构造公共请求参数
     */
    public Map<String, Object> buildPublicParams(Object businessData) {
        String jsonStr = JsonUtils.toJsonString(businessData);
        // data 为 JSON 参数进行 Base64 后的 URL 编码
        String dataBase64 = URLEncoder.encode(Base64.encodeBase64String(jsonStr.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        long timestamp = System.currentTimeMillis();
        String accessToken = getAccessToken();
        String sign = generateSign(accessToken, dataBase64, timestamp);

        Map<String, Object> params = new HashMap<>();
        params.put("data", dataBase64);
        params.put("timestamp", timestamp);
        params.put("access_token", accessToken);
        params.put("sign", sign);
        return params;
    }


    /**
     * 获取 AccessToken
     * 文档要求密码为大写 MD5
     */
    public AccessTokenResp getAccessToken(String authUser, String authPassword) {
        String md5Password = DigestUtils.md5Hex(authPassword).toUpperCase();
        JsonNode root = Req.get(threeMeetingProps.getPrefix()).path("/openapi/v1/token")
                .query("auth_user", authUser)
                .query("auth_password", md5Password)
                .query("app_id", threeMeetingProps.getAppId())
                .query("is_reuse", "1")
                .ok()
                .node();
        JsonNode data = root.get("data");

        return Json.toObj(data, AccessTokenResp.class);
    }

    /**
     * 获取邀请会议的 Token
     * 用于获取加入特定会议室所需的 room_token
     */
    @SneakyThrows
    public String getRoomToken(GetRoomTokenReq req) {
        Map<String, Object> data = buildPublicParams(req);
        JsonNode root = Req.post(threeMeetingProps.getPrefix())
                .path("/openapi/v1/rooms/token/get")
                .query(data)
                .ok()
                .node();

        JsonNode roomTokenNode = root.findPath("room_token");

        return roomTokenNode.asText();
    }

    /**
     * 呼叫加入视频调度 (POST 请求)
     * 需要进行复杂的签名校验 [cite: 21, 40]
     */
    public InviteResp inviteToMeeting(InviteReq inviteReq) {
        Map<String, Object> data = buildPublicParams(inviteReq);
        JsonNode root = Req.post(threeMeetingProps.getPrefix())
                .path("/openapi/v1/rooms/vds/invite")
                .query(data)
                .ok()
                .node();

        JsonNode res = root.get("data");
        return Json.toObj(res, InviteResp.class);
    }

    /**
     * 查询呼叫结果
     * 根据呼叫时返回的 call_sn 批量查询状态
     */
    public List<CallResultResp> queryCallResult(List<String> callSnArray) {
        QueryCallResultReq req = new QueryCallResultReq();
        req.setCallSn(callSnArray);

        Map<String, Object> data = buildPublicParams(req);
        JsonNode root = Req.post(threeMeetingProps.getPrefix())
                .path("/openapi/v1/rooms/vds/invite/result")
                .query(data)
                .ok()
                .node();
        JsonNode res = root.get("data");

        return Json.toList(res, CallResultResp.class);
    }


}
