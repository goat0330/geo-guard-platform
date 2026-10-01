/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.security.Security;

public class SM2Util {

    static {
        // 注册 Bouncy Castle 服务
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * SM2 加密
     *
     * @param msg          明文
     * @param publicKeyHex 16进制公钥字符串
     * @return 16进制加密结果
     */
    public static String encrypt(String msg, String publicKeyHex) throws Exception {
        // 1. 获取国密 SM2 曲线参数
        X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
        ECDomainParameters domainParameters = new ECDomainParameters(
                x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN(), x9ECParameters.getH());

        // 2. 解析公钥 (Hex 转换成 EC 坐标点)
        byte[] publicKeyBytes = Hex.decode(publicKeyHex);
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(
                x9ECParameters.getCurve().decodePoint(publicKeyBytes), domainParameters);

        // 3. 设置加密引擎
        // Mode.C1C3C2 对应前端 cipherMode = 1
        SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(true, new ParametersWithRandom(publicKeyParameters));

        // 4. 执行加密
        byte[] in = msg.getBytes("UTF-8");
        byte[] out = engine.processBlock(in, 0, in.length);

        // 5. 返回 Hex 格式 (前端 sm-crypto 默认返回 Hex)
        return Hex.toHexString(out);
    }

    public static void main(String[] args) {
        try {
            String publicKey = "04711ba4975e07470b0832ea0f5857b66199fdba522f4a0a83e99e2226e1dc58eb31801093bcc058af42bbd17e0bf159916f6ac2fa93d3c2f33b593ca49cab8eaf";
            String msg = "Jyxszdd666";

            String encryptData = encrypt(msg, publicKey);
            System.out.println("加密结果: " + encryptData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
