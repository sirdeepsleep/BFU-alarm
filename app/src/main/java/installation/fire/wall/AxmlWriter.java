package installation.fire.wall;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AxmlWriter {

    public static byte[] generateManifest(String packageName, String targetPermPackage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<String> strings = new ArrayList<>();
        strings.add("hasCode");                   // 0
        strings.add("sharedUserId");              // 1
        strings.add("minSdkVersion");             // 2
        strings.add("targetSdkVersion");          // 3
        strings.add("versionCode");               // 4
        strings.add("versionName");               // 5
        strings.add("compileSdkVersion");         // 6
        strings.add("compileSdkVersionCodename"); // 7
                
        strings.add("name");                      // 8 (0x01010003)
        strings.add("protectionLevel");           // 9 (0x01010009)
        
        strings.add("http://schemas.android.com/apk/res/android"); // 10
        strings.add("android");                   // 11
        strings.add("manifest");                  // 12
        strings.add("uses-sdk");                  // 13
        strings.add("application");               // 14
        strings.add("package");                   // 15
        strings.add("platformBuildVersionCode");  // 16
        strings.add("platformBuildVersionName");  // 17
        strings.add("1.0");                       // 18
        strings.add("14");                        // 19
        strings.add(packageName);                 // 20        
        
        boolean addPerm = targetPermPackage != null && !targetPermPackage.isEmpty();
        
        if (addPerm) {
            strings.add("permission");                // 21
            strings.add(targetPermPackage + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"); // 22
        }

        ByteArrayOutputStream body = new ByteArrayOutputStream();

        writeStringPool(body, strings);
        writeResourceMap(body);
        writeNamespace(body, 0x0100, 11, 10);
        writeStartManifest(body, 20);
        writeUsesSdk(body);
        writeEndTag(body, 13); 

        if (addPerm) {
            writePermission(body, 22);
            writeEndTag(body, 21);
        }

        writeApplication(body);
        writeEndTag(body, 14);  

        writeEndTag(body, 12);
        writeNamespace(body, 0x0101, 11, 10);

        byte[] bodyBytes = body.toByteArray();

        writeShort(out, 0x0003); 
        writeShort(out, 8);      
        writeInt(out, bodyBytes.length + 8);
        out.write(bodyBytes);

        return out.toByteArray();
    }

    private static void writePermission(OutputStream os, int permNameValIdx) throws IOException {
        int chunkSize = 36 + (2 * 20);

        writeShort(os, 0x0102); 
        writeShort(os, 16);
        writeInt(os, chunkSize);
        writeInt(os, 1);
        writeInt(os, -1);
        writeInt(os, -1); 
        writeInt(os, 21);
        writeShort(os, 20); 
        writeShort(os, 20); 
        writeShort(os, 2);
        writeShort(os, 0);
        writeShort(os, 0);
        writeShort(os, 0);

        writeAttribute(os, 10, 8, permNameValIdx, 0x03, permNameValIdx);
        writeAttribute(os, 10, 9, -1, 0x10, 2);
    }

    private static void writeStringPool(OutputStream os, List<String> strings) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        int[] offsets = new int[strings.size()];

        for (int i = 0; i < strings.size(); i++) {
            offsets[i] = data.size();
            String s = strings.get(i);
            writeShort(data, s.length());
            byte[] bytes = s.getBytes(StandardCharsets.UTF_16LE);
            data.write(bytes);
            writeShort(data, 0);
        }

        while (data.size() % 4 != 0) {
            data.write(0);
        }

        byte[] stringData = data.toByteArray();
        int headerSize = 28;
        int chunkSize = headerSize + (strings.size() * 4) + stringData.length;

        writeShort(os, 0x0001); 
        writeShort(os, headerSize);
        writeInt(os, chunkSize);
        writeInt(os, strings.size());
        writeInt(os, 0); 
        writeInt(os, 0); 
        writeInt(os, headerSize + (strings.size() * 4)); 
        writeInt(os, 0); 

        for (int offset : offsets) {
            writeInt(os, offset);
        }
        os.write(stringData);
    }

    private static void writeResourceMap(OutputStream os) throws IOException {
        writeShort(os, 0x0180); 
        writeShort(os, 8);
        writeInt(os, 8 + (10 * 4));
        writeInt(os, 0x0101000c);  
        writeInt(os, 0x0101000b);  
        writeInt(os, 0x0101020c);  
        writeInt(os, 0x01010270);  
        writeInt(os, 0x0101021b);  
        writeInt(os, 0x0101021c);  
        writeInt(os, 0x01010572);  
        writeInt(os, 0x01010573);  
        writeInt(os, 0x01010003);
        writeInt(os, 0x01010009);
    }

    private static void writeNamespace(OutputStream os, int type, int prefixIdx, int uriIdx) throws IOException {
        writeShort(os, type);
        writeShort(os, 16);
        writeInt(os, 24);
        writeInt(os, 1);  
        writeInt(os, -1); 
        writeInt(os, prefixIdx);
        writeInt(os, uriIdx);
    }

    private static void writeStartManifest(OutputStream os, int pkgNameValIdx) throws IOException {
        int attrCount = 7;
        int chunkSize = 36 + (attrCount * 20);

        writeShort(os, 0x0102); 
        writeShort(os, 16);
        writeInt(os, chunkSize);
        writeInt(os, 1);
        writeInt(os, -1);
        writeInt(os, -1); 
        writeInt(os, 12); 
        writeShort(os, 20); 
        writeShort(os, 20); 
        writeShort(os, attrCount);
        writeShort(os, 0);
        writeShort(os, 0);
        writeShort(os, 0);

        writeAttribute(os, -1, 15, pkgNameValIdx, 0x03, pkgNameValIdx);
        writeAttribute(os, 10, 4, -1, 0x10, 1);
        writeAttribute(os, 10, 5, 18, 0x03, 18);
        writeAttribute(os, 10, 6, -1, 0x10, 34);
        writeAttribute(os, 10, 7, 19, 0x03, 19);
        writeAttribute(os, -1, 16, -1, 0x10, 34);
        writeAttribute(os, -1, 17, 19, 0x03, 19);
    }

    private static void writeUsesSdk(OutputStream os) throws IOException {
        int chunkSize = 36 + (2 * 20);

        writeShort(os, 0x0102);
        writeShort(os, 16);
        writeInt(os, chunkSize);
        writeInt(os, 1);
        writeInt(os, -1);
        writeInt(os, -1);
        writeInt(os, 13); 
        writeShort(os, 20);
        writeShort(os, 20);
        writeShort(os, 2); 
        writeShort(os, 0);
        writeShort(os, 0);
        writeShort(os, 0);

        writeAttribute(os, 10, 2, -1, 0x10, 1);
        writeAttribute(os, 10, 3, -1, 0x10, 37);
    }

    private static void writeApplication(OutputStream os) throws IOException {
        int chunkSize = 36 + 20;

        writeShort(os, 0x0102);
        writeShort(os, 16);
        writeInt(os, chunkSize);
        writeInt(os, 1);
        writeInt(os, -1);
        writeInt(os, -1);
        writeInt(os, 14); 
        writeShort(os, 20);
        writeShort(os, 20);
        writeShort(os, 1); 
        writeShort(os, 0);
        writeShort(os, 0);
        writeShort(os, 0);

        writeAttribute(os, 10, 0, -1, 0x12, 0xFFFFFFFF);
    }

    private static void writeEndTag(OutputStream os, int nameIdx) throws IOException {
        writeShort(os, 0x0103); 
        writeShort(os, 16);
        writeInt(os, 24);
        writeInt(os, 1);
        writeInt(os, -1);
        writeInt(os, -1);
        writeInt(os, nameIdx);
    }

    private static void writeAttribute(OutputStream os, int ns, int name, int rawVal, int dataType, int data) throws IOException {
        writeInt(os, ns);
        writeInt(os, name);
        writeInt(os, rawVal);
        writeShort(os, 8); 
        os.write(0);       
        os.write(dataType);
        writeInt(os, data);
    }

    private static void writeShort(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    private static void writeInt(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
}
