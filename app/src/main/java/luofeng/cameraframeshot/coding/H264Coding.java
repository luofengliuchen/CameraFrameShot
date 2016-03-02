package luofeng.cameraframeshot.coding;

import android.hardware.Camera;

import luofeng.cameraframeshot.nativecode.H264Decoder;

/**
 * Created by 美工 on 2016/3/2.
 */
public class H264Coding implements Camera.PreviewCallback{


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        new H264Decoder(H264Decoder.COLOR_FORMAT_YUV420).decodeFrameToDirectBuffer();
    }


}
