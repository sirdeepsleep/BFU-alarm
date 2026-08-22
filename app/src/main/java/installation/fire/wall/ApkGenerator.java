package installation.fire.wall;

import android.content.Context;

import com.android.apksig.ApkSigner;
import com.android.apksig.util.DataSources;
import com.android.apksig.util.DataSinks;
import com.android.apksig.util.ReadableDataSink;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.security.auth.x500.X500Principal;

public class ApkGenerator {

    private final Context context;

    private static final String DEX_HEX = 
        "6465780a30333800d3b0c7feb30ac36b" +
        "723806b859c6b0f045f2769a38000000" +
        "74000000785634120000000000000000" +
        "fc000000040000007000000002000000" +
        "14000000000000000100000018000000" +
        "00000000010000002c00000000000000" +
        "00000000000000000100000001000000" +
        "40000000000000004200000001000000" +
        "0000000046000000010000004e000000" +
        "010000005c0000000100000064000000" +
        "4c616e79456d7074793b00004c6a6176" +
        "612f6c616e672f4f626a6563743b0000" +
        "456d7074790000000100000002000000" +
        "03000000000000000100000000000000" +
        "00000000000000000000000000000000" +
        "00000000000000000000000000000000" +
        "0000000000000000";

    public ApkGenerator(Context context) {
        this.context = context;
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static byte[] getEmptyDexBytes() {
        int len = DEX_HEX.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(DEX_HEX.charAt(i), 16) << 4)
                                 + Character.digit(DEX_HEX.charAt(i+1), 16));
        }
        return data;
    }

    public byte[] generateBytes(String targetPackage, String targetPermPackage) throws Exception {
        ByteArrayOutputStream unsignedBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(unsignedBaos)) {
            zos.putNextEntry(new ZipEntry("classes.dex"));
            zos.write(getEmptyDexBytes());
            zos.closeEntry();

            byte[] binaryAxml = AxmlWriter.generateManifest(targetPackage, targetPermPackage);
            zos.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zos.write(binaryAxml);
            zos.closeEntry();
        }
        byte[] unsignedApkBytes = unsignedBaos.toByteArray();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        String randomAuthor = "Author_" + UUID.randomUUID().toString().substring(0, 6);
        X500Principal owner = new X500Principal("CN=" + randomAuthor + ", OU=Dev, O=FireWall, C=US");
        Date notBefore = new Date(System.currentTimeMillis() - 86400000L);
        Date notAfter = new Date(System.currentTimeMillis() + 864000000000L);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                owner, BigInteger.valueOf(System.currentTimeMillis()),
                notBefore, notAfter, owner, keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());
        
        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(certHolder);

        ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(
                "KEY", keyPair.getPrivate(), Collections.singletonList(certificate))
                .build();

        ReadableDataSink signedSink = DataSinks.newInMemoryDataSink();

        ApkSigner apkSigner = new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(DataSources.asDataSource(ByteBuffer.wrap(unsignedApkBytes)))
                .setOutputApk(signedSink)
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build();

        apkSigner.sign();

        byte[] signedApkBytes = new byte[(int) signedSink.size()];
        signedSink.copyTo(0, signedApkBytes.length, ByteBuffer.wrap(signedApkBytes));

        return signedApkBytes;
    }
}
