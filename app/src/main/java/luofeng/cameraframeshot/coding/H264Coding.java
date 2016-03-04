package luofeng.cameraframeshot.coding;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import java.io.ByteArrayOutputStream;

import luofeng.cameraframeshot.nativecode.H264Decoder;

/**
 * Created by 美工 on 2016/3/2.
 */
public class H264Coding implements Camera.PreviewCallback{


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        new H264Decoder(H264Decoder.COLOR_FORMAT_YUV420).decodeFrameToDirectBuffer();
        Camera.Size sizes = camera.getParameters().getPreviewSize();
        byte[] target = decodeYUV(data,sizes.width,sizes.height);

        //将一帧一帧数据分开传递


    }

    private byte[] decodeYUV(byte[] yuv, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        byte[] imageBytes = out.toByteArray();
        return imageBytes;
    }


}
