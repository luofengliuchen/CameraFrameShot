package luofeng.cameraframeshot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    // 定义对象
    private SurfaceView mSurfaceview = null;        // SurfaceView对象：(视图组件)视频显示
    private SurfaceHolder mSurfaceHolder = null;    // SurfaceHolder对象：(抽象接口)SurfaceView支持类
    private Camera mCamera =null;                   // Camera对象，相机预览
    private String TAG = "camera";
    private boolean bIfPreview;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private Button switchButton;
    private boolean isFrontCamera = true;
    public static final int MSG = 1;
    public LinearLayout container;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            String msgObject = (String) msg.obj;
            switch (msg.what){
                case MSG:

                    if(container.getChildCount()>200){
                        container.removeAllViews();
                    }

                    TextView tv = new TextView(MainActivity.this);
                    tv.setTextColor(Color.BLACK);
                    tv.setText(msgObject);
                    container.addView(tv);
                    break;
            }
            super.handleMessage(msg);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switchButton = (Button) findViewById(R.id.btn_switch);
        container = (LinearLayout) findViewById(R.id.ll_container);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                isFrontCamera = !isFrontCamera;
//                releaseCamera();
//                createCamera(isFrontCamera);
//                initCamera();
                capture();
            }
        });
        initSurfaceView();
    }


    public static Camera openBackCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return Camera.open(i);
            }
        }
        return null;
    }

    public static Camera openFrontCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return Camera.open(i);
            }
        }
        return null;
    }


    private void createCamera(boolean isFront){
        // 开启摄像头（2.3版本后支持多摄像头,需传入参数）
        //设置是开启前置或是后置摄像头
        if(isFront){
            mCamera = openFrontCamera();
        }else{
            mCamera = openBackCamera();
        }

        try{
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (Exception ex)
        {
            if(null != mCamera)
            {
                mCamera.release();
                mCamera = null;
            }
            Log.i(TAG+"initCamera", ex.getMessage());
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG,"surfaceCreated");
        createCamera(isFrontCamera);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        Log.i(TAG, "SurfaceHolder.Callback：Surface Changed");
        //mPreviewHeight = height;
        //mPreviewWidth = width;
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        releaseCamera();
    }

    private void releaseCamera(){
        Log.i(TAG, "SurfaceHolder.Callback：Surface Destroyed");
        if(null != mCamera)
        {
            mCamera.setPreviewCallback(null); //！！这个必须在前，不然退出出错
            mCamera.stopPreview();
            bIfPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /*【2】【相机预览】*/
    private void initCamera()//surfaceChanged中调用
    {
        Log.i(TAG, "going into initCamera");
        if (bIfPreview) {
            mCamera.stopPreview();//stopCamera();
        }
        if (null != mCamera) {
            try {
                /* Camera Service settings*/
                Camera.Parameters parameters = mCamera.getParameters();
                // parameters.setFlashMode("off"); // 无闪光灯
                parameters.setPictureFormat(PixelFormat.JPEG); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); //Sets the image format for preview picture，默认为NV21
                /*【ImageFormat】JPEG/NV16(YCrCb format，used for Video)/NV21(YCrCb format，used for Image)/RGB_565/YUY2/YU12*/

                // 【调试】获取caera支持的PictrueSize，看看能否设置？？
                List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
                List<Integer> previewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
                Log.i(TAG + "initCamera", "cyy support parameters is ");
                Camera.Size psize = null;
                for (int i = 0; i < pictureSizes.size(); i++) {
                    psize = pictureSizes.get(i);
                    Log.i(TAG + "initCamera", "PictrueSize,width: " + psize.width + " height" + psize.height);
                }
                for (int i = 0; i < previewSizes.size(); i++) {
                    psize = previewSizes.get(i);
                    Log.i(TAG + "initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
                }
                Integer pf = null;
                for (int i = 0; i < previewFormats.size(); i++) {
                    pf = previewFormats.get(i);
                    Log.i(TAG + "initCamera", "previewformates:" + pf);
                }


                WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getMetrics(displayMetrics);


                Camera.Size previewSize = getOptimalPreviewSize(
                        previewSizes,
                        display.getWidth(),
                        display.getHeight());


                // 设置拍照和预览图片大小
                parameters.setPictureSize(previewSize.width, previewSize.height); //指定拍照图片的大小
                parameters.setPreviewSize(previewSize.width, previewSize.height); // 指定preview的大小
                //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错

                // 横竖屏镜头自动调整
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait"); //
                    parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
                    mCamera.setDisplayOrientation(90); // 在2.2以上可以使用
                } else// 如果是横屏
                {
                    parameters.set("orientation", "landscape"); //
                    mCamera.setDisplayOrientation(0); // 在2.2以上可以使用
            }

                /* 视频流编码处理 */
                //添加对视频流处理函数
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {


                        Message msg = Message.obtain();
                        msg.obj = "摄像头帧处理数据大小:---"+data.length;
                        msg.what = MSG;
                        mHandler.sendMessage(msg);
                    }
                });


                // 设定配置参数并开启预览
                mCamera.setParameters(parameters); // 将Camera.Parameters设定予Camera
                mCamera.startPreview(); // 打开预览画面
                bIfPreview = true;

                // 【调试】设置后的图片大小和预览大小以及帧率
                Camera.Size csize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = csize.height; //
                mPreviewWidth = csize.width;
                Log.i(TAG + "initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
                csize = mCamera.getParameters().getPictureSize();
                Log.i(TAG + "initCamera", "after setting, pictruesize:width: " + csize.width + " height: " + csize.height);
                Log.i(TAG + "initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
                Log.i(TAG + "initCamera", "after setting, previewframetate is " + mCamera.getParameters().getPreviewFrameRate());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // InitSurfaceView
    private void initSurfaceView(){
        mSurfaceview = (SurfaceView)findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
        // mSurfaceHolder.setFixedSize(176, 144); // 预览大小設置
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//設置顯示器類型，setType必须设置
    }


    public void capture() {
        if (mCamera != null) {
            // 控制摄像头自动对焦后才拍摄
            mCamera.autoFocus(autoFocusCallback);
        }
    }


    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            if (arg0) {
                // takePicture()方法需要传入三个监听参数
                // 第一个监听器；当用户按下快门时激发该监听器
                // 第二个监听器；当相机获取原始照片时激发该监听器
                // 第三个监听器；当相机获取JPG照片时激发该监听器
                mCamera.takePicture(new Camera.ShutterCallback() {

                    @Override
                    public void onShutter() {
                        // 按下快门瞬间会执行此处代码
                    }
                }, new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] arg0, Camera arg1) {
                        // 此处代码可以决定是否需要保存原始照片信息
                    }
                }, myJpegCallback);
            }

        }
    };




    Camera.PictureCallback myJpegCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Message msg = Message.obtain();
            msg.obj = "摄像头帧处理数据大小:---"+data.length;
            msg.what = MSG;
            mHandler.sendMessage(msg);

            // 根据拍照所得的数据创建位图
            final Bitmap bm = BitmapFactory.decodeByteArray(data, 0,
                    data.length);
            // 加载布局文件
            View saveDialog = getLayoutInflater().inflate(R.layout.save, null);
            final EditText potoName = (EditText) saveDialog
                    .findViewById(R.id.photoNmae);
            // 获取saveDialog对话框上的ImageView组件
            ImageView show = (ImageView) saveDialog.findViewById(R.id.show);
            // 显示刚刚拍得的照片
            show.setImageBitmap(bm);
            // 使用AlertDialog组件
            new AlertDialog.Builder(MainActivity.this)
                    .setView(saveDialog)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("保存",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    // 创建一个位于SD卡上的文件
                                    File file = new File(Environment
                                            .getExternalStorageDirectory()
                                            + "/"
                                            + potoName.getText().toString()
                                            + ".jpg");
                                    FileOutputStream fileOutStream=null;
                                    try {
                                        fileOutStream=new FileOutputStream(file);
                                        //把位图输出到指定的文件中
                                        bm.compress(Bitmap.CompressFormat.JPEG, 100, fileOutStream);
                                        fileOutStream.close();
                                    } catch (IOException io) {
                                        io.printStackTrace();
                                    }

                                }
                            }).show();
            //重新浏览
            camera.stopPreview();
            camera.startPreview();
            bIfPreview=true;
        }
    };


}
