package wiki.hike.neo.hikecamera.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

import wiki.hike.neo.hikecamera.R;

/**
 * Created by Neo on 09/10/17.
 */
//This is the Baap of all the filters

public class Filter {

    public static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    public static final int RENDER_TYPE_SURFACE_TEXTURE = 0;
    public static final int RENDER_TYPE_PREVIEW_BUFFER = 1;
    public static final int RENDER_TYPE_SAMPLER2D = 2;

    protected int mRenderType = RENDER_TYPE_SAMPLER2D;

    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform lowp sampler2D texSampler;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(texSampler, v_texcoord);\n" +
            "    gl_FragColor = color;\n" +
            //"    gl_FragColor = vec4(1.0,0.0,0.0,0.0);\n"+
            "}";

    public static final String NO_FILTER_VERTEX_SHADER= "" +
            //"uniform mat4 u_MVPMatrix;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            //    "gl_Position = u_MVPMatrix * a_position;\n"+
            "gl_Position =  a_position;\n" +
            "v_texcoord = a_texcoord;\n" +
            "}";


    private LinkedList<Runnable> mRunOnDraw = null;
    protected int mGLProgId;

    protected final int SHORT_SIZE_BYTES = 2;
    protected final int FLOAT_SIZE_BYTES = 4;

    protected final static String A_POSITION = "a_position";
    protected final static String A_TEXCOORD = "a_texcoord";
    //protected final static String U_MVPMATRIX = "u_MVPMatrix";
    protected final static String U_SAMPLER0 = "texSampler";
    protected final static String U_LUMINANCE_SAMPLER = "luminanceTexture";
    protected final static String U_CHROMINANCE_SAMPLER = "chrominanceTexture";


    protected int maPositionHandle;
    protected int maTextureHandle;
    protected int muMVPMatrixHandle;
    protected int muSampler0Handle;
    protected int mLumninanceSampler;
    protected int mChrominanceSampler;

    private float[] mProjMatrix = new float[16];

    private String mVertexShader;
    private String mFragmentShader;


    private int[] mTextureConstant = {GLES20.GL_TEXTURE0,GLES20.GL_TEXTURE1};
    private int[] mSamplers = null;

    private boolean mIsInitialized;


    public Filter() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        mIsInitialized = false;
    }

    public Filter(String vertexShader, String fragmentShader) {
        mRunOnDraw = new LinkedList<Runnable>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public int getRenderType(){
        return mRenderType;
    }

    public final void init()
    {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    public void onInit()
    {
        //super.onInit();
        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);

        Matrix.orthoM(mProjMatrix, 0, -1, 1, 1, -1, 1, -1);
        maPositionHandle = GLES20.glGetAttribLocation(mGLProgId, A_POSITION);
        maTextureHandle = GLES20.glGetAttribLocation(mGLProgId, A_TEXCOORD);
        //muMVPMatrixHandle = GLES20.glGetUniformLocation(mGLProgId, U_MVPMATRIX);

        switch (getRenderType())
        {
            case RENDER_TYPE_PREVIEW_BUFFER:
                mLumninanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_LUMINANCE_SAMPLER);
                mChrominanceSampler = GLES20.glGetUniformLocation(mGLProgId, U_CHROMINANCE_SAMPLER);
                mSamplers = new int[2];
                mSamplers[0] = mLumninanceSampler;
                mSamplers[1] = mChrominanceSampler;
                break;
            case RENDER_TYPE_SURFACE_TEXTURE:
            case RENDER_TYPE_SAMPLER2D:
                muSampler0Handle = GLES20.glGetUniformLocation(mGLProgId, U_SAMPLER0);
                mSamplers = new int[1];
                mSamplers[0] = muSampler0Handle;
                break;
        }
    }

    public void onInitialized() {

    }

    public void onDraw(final int[] textureArr , int vertexBufferObjectId, int elementBufferObjectId)
    {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        for(int i=0;i<textureArr.length;i++)
        {
            GLES20.glActiveTexture(mTextureConstant[i]);
            GLES20.glBindTexture((getRenderType() == RENDER_TYPE_SURFACE_TEXTURE) ? GL_TEXTURE_EXTERNAL_OES: GLES20.GL_TEXTURE_2D , textureArr[i]);
            GLES20.glUniform1i(mSamplers[i], i);
        }

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTextureHandle);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferObjectId);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, true, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);

        onDrawArraysPre();
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, elementBufferObjectId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * SHORT_SIZE_BYTES, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glDisableVertexAttribArray(maPositionHandle);

        GLES20.glFinish();
        GLES20.glUseProgram(0);
    }

    protected void onDrawArraysPre() {
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    public void onDestroy() {
    }


}
