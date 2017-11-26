/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.demo.util.tootle;

import org.joml.*;
import org.lwjgl.*;
import org.lwjgl.assimp.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.Platform;
import org.lwjgl.util.par.*;

import java.nio.*;
import java.nio.file.*;
import java.util.concurrent.*;

import static java.lang.Math.*;
import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.stb.STBEasyFont.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.nfd.NativeFileDialog.*;
import static org.lwjgl.util.par.ParShapes.*;
import static org.lwjgl.util.tootle.Tootle.*;

public final class HelloTootle {

    private final long window;

    private int width  = 1024;
    private int height = 768;

    private ParShapesMesh mesh;
    private ParShapesMesh meshOptimized;

    private boolean hasNormals;

    private float acmr;
    private float acmrOptimized;
    private float ovdr;
    private float ovdrOptimized;

    private final Callback debugCB;

    private final int vao;
    private final int vboMesh;
    private final int iboMesh;

    private final int programMesh;
    private final int uniformModelViewProjectionMatrix;
    private final int uniformNormalMatrix;
    private final int uniformCubeSize;

    private final Matrix4d modelViewMatrix  = new Matrix4d();
    private final Matrix4d projectionMatrix = new Matrix4d();

    private final Matrix4d modelViewProjectionMatrix = new Matrix4d();
    private final Matrix3d normalMatrix              = new Matrix3d();

    private int meshKey = 1;

    private int
        slices       = 32,
        stacks       = 32,
        seed         = 1,
        subdivisions = 4;

    private int cubeSize;

    private final AIPropertyStore propertyStore;

    private int     vcacheMethod         = Platform.get() == Platform.WINDOWS ? 2 : 0;
    private int     vcacheSize           = TOOTLE_DEFAULT_VCACHE_SIZE;
    private boolean optimizeVertexMemory = true;

    private boolean fragmentShader = true;
    private boolean optimized      = true;
    private boolean wireframe;

    private final int vboHUD;
    private       int vboHUDVertexCount;
    private final int vboPerf;

    private final int programHUD;
    private final int uniformModelViewProjectionMatrixHUD;

    private final Matrix4d modelViewProjectionMatrixHUD = new Matrix4d();

    private final PerfGraph gpuGraph;
    private final GPUTimer  gpuTimer;

    private HelloTootle() {
        GLFWErrorCallback.createPrint().set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);
        }
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "AMD Tootle demo", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwMakeContextCurrent(window);
        GLCapabilities caps = GL.createCapabilities();
        if (!caps.OpenGL33) {
            throw new IllegalStateException("OpenGL 3.3 is required to run this demo.");
        }
        debugCB = GLUtil.setupDebugMessageCallback();
        if (debugCB != null) {
            if (caps.OpenGL43) {
                glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer)null, false);
            } else if (caps.GL_KHR_debug) {
                KHRDebug.glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer)null, false);
            }
        }

        glfwSwapInterval(0);

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action != GLFW_RELEASE) {
                return;
            }

            int scale;
            if ((mods & GLFW_MOD_CONTROL) != 0) {
                scale = 100;
            } else if ((mods & GLFW_MOD_SHIFT) != 0) {
                scale = 10;
            } else {
                scale = 1;
            }
            switch (key) {
                case GLFW_KEY_DOWN:
                    if (slices > 3) {
                        slices -= scale;
                        if (slices < 3) {
                            slices = 3;
                        }
                        updateMesh();
                    }
                    break;
                case GLFW_KEY_UP:
                    slices += scale;
                    updateMesh();
                    break;
                case GLFW_KEY_LEFT:
                    if (stacks > 1) {
                        stacks -= scale;
                        if (stacks < 1) {
                            stacks = 1;
                        }
                        updateMesh();
                    }
                    break;
                case GLFW_KEY_RIGHT:
                    stacks += scale;
                    updateMesh();
                    break;
                case GLFW_KEY_PAGE_DOWN:
                    if (meshKey != GLFW_KEY_8) {
                        seed--;
                        updateMesh();
                    } else {
                        if (subdivisions > 1) {
                            subdivisions--;
                            updateMesh();
                        }
                    }
                    break;
                case GLFW_KEY_PAGE_UP:
                    if (meshKey != GLFW_KEY_8) {
                        seed++;
                        updateMesh();
                    } else {
                        subdivisions++;
                        updateMesh();
                    }
                    break;
                case GLFW_KEY_KP_SUBTRACT:
                    if ((mods & GLFW_MOD_CONTROL) != 0) {
                        if (1 < cubeSize) {
                            setCubeSize(cubeSize - 1);
                        }
                    } else if (16 < vcacheSize) {
                        vcacheSize -= 8;
                        setMesh(mesh);
                    }
                    break;
                case GLFW_KEY_KP_ADD:
                    if ((mods & GLFW_MOD_CONTROL) != 0) {
                        setCubeSize(cubeSize + 1);
                    } else if (vcacheSize < 32) {
                        vcacheSize += 8;
                        setMesh(mesh);
                    }
                    break;
                case GLFW_KEY_1:
                case GLFW_KEY_2:
                case GLFW_KEY_3:
                case GLFW_KEY_4:
                case GLFW_KEY_5:
                case GLFW_KEY_6:
                case GLFW_KEY_7:
                case GLFW_KEY_8:
                    updateMesh(key);
                    break;
                case GLFW_KEY_F1:
                case GLFW_KEY_F2:
                case GLFW_KEY_F3:
                    vcacheMethod = key - GLFW_KEY_F1;
                    setMesh(mesh);
                    break;
                case GLFW_KEY_F4:
                    optimizeVertexMemory = !optimizeVertexMemory;
                    setMesh(mesh);
                    break;
                case GLFW_KEY_F:
                    fragmentShader = !fragmentShader;
                    updateHUD();
                    break;
                case GLFW_KEY_O:
                    if ((mods & GLFW_MOD_CONTROL) != 0) {
                        meshKey = -1;
                        importMesh();
                    } else {
                        optimized = !optimized;
                        if (mesh != null) {
                            updateMeshGPU();
                        }
                        updateHUD();
                    }
                    break;
                case GLFW_KEY_W:
                    wireframe = !wireframe;
                    updateHUD();
                    break;
                case GLFW_KEY_ESCAPE:
                    glfwSetWindowShouldClose(window, true);
                    break;
                case GLFW_KEY_SPACE:
                    if (mesh != null) {
                        shuffleMesh();
                    }
                    break;
            }
        });

        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            updateViewport(width, height);
        });

        // center window
        long monitor = glfwGetPrimaryMonitor();

        GLFWVidMode vidmode = glfwGetVideoMode(monitor);

        glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        );

        glfwShowWindow(window);

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glEnable(GL_CULL_FACE);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        updateViewport(width, height);

        vao = glGenVertexArrays();
        glBindVertexArray(vao); // bind and ignore

        vboMesh = glGenBuffers();
        iboMesh = glGenBuffers();
        vboHUD = glGenBuffers();
        vboPerf = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vboPerf);
        glBufferData(GL_ARRAY_BUFFER, 4 * 1024, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        updateMesh(GLFW_KEY_1);
        setCubeSize(8);

        programMesh = buildShaderProgram(
            "#version 330\n" +
            "\n" +
            "uniform mat4 mMVP;\n" +
            "uniform mat3 mNORMAL;\n" +
            "uniform int cubeSize;\n" +
            "\n" +
            "layout(location = 0) in vec3 iPosition;\n" +
            "layout(location = 1) in vec3 iNormal;\n" +
            "\n" +
            "out vec3 vNormal;\n" +
            "\n" +
            "void main(void) {\n" +
            "  int x = gl_InstanceID / (cubeSize * cubeSize);\n" +
            "  int y = (gl_InstanceID / cubeSize) % cubeSize;\n" +
            "  int z = gl_InstanceID % cubeSize;\n" +
            "  float offset = -(cubeSize / 2);\n" +
            "  if (cubeSize + offset * 2 == 0) {\n" +
            "    offset += 0.5;" +
            "  }\n" +
            "  gl_Position = mMVP * vec4(iPosition + vec3(offset + float(x), offset + float(y), offset + float(z)), 1.0);\n" +
            "  vNormal = mNORMAL * iNormal;\n" +
            "}\n",
            "#version 330\n" +
            "\n" +
            "in vec3 vNormal;\n" +
            "\n" +
            "layout(location = 0) out vec4 oColor;\n" +
            "\n" +
            "void main(void) {\n" +
            "  oColor = vec4(normalize(vNormal), 1.0);\n" +
            "}\n"
        );
        uniformModelViewProjectionMatrix = glGetUniformLocation(programMesh, "mMVP");
        uniformNormalMatrix = glGetUniformLocation(programMesh, "mNORMAL");
        uniformCubeSize = glGetUniformLocation(programMesh, "cubeSize");

        programHUD = buildShaderProgram(
            "#version 330\n" +
            "\n" +
            "uniform mat4 mMVP;\n" +
            "\n" +
            "layout(location = 0) in vec2 iPosition;\n" +
            "layout(location = 1) in vec4 iColor;\n" +
            "\n" +
            "out vec4 vColor;\n" +
            "\n" +
            "void main(void) {\n" +
            "  gl_Position = mMVP * vec4(iPosition, 0.0, 1.0);\n" +
            "  vColor = iColor;\n" +
            "}\n",
            "#version 330\n" +
            "\n" +
            "in vec4 vColor;\n" +
            "\n" +
            "layout(location = 0) out vec4 oColor;\n" +
            "\n" +
            "void main(void) {\n" +
            "  oColor = vColor;\n" +
            "}\n"
        );
        uniformModelViewProjectionMatrixHUD = glGetUniformLocation(programHUD, "mMVP");

        gpuGraph = new PerfGraph();
        gpuTimer = new GPUTimer();

        propertyStore = aiCreatePropertyStore();
        aiSetImportPropertyInteger(propertyStore, AI_CONFIG_PP_PTV_NORMALIZE, 1);
    }

    public static void main(String[] args) {
        System.out.println("Press <SPACE> to shuffle the index buffer of the unoptimized mesh.");
        System.out.println("Press <CTRL+O> to load a custom mesh using Assimp.");
        System.out.println("Press <F1> to perform ACMR optimization.");
        System.out.println("Press <F2> to perform slow ACMR+Overdraw optimization.");
        System.out.println("Press <F3> to perform fast ACMR+Overdraw optimization.");
        System.out.println("Press <F4> to toggle vertex prefetch cache optimization.");
        System.out.println("Press <F> to toggle fragment shader (ON: full viewport, OFF: null viewport).");
        System.out.println("\tACMR is best measured with FS OFF");
        System.out.println("\tOVDR is best measured with FS ON");
        System.out.println("-----------------------------------------------------------------------------");

        TootleInit();
        try {
            new HelloTootle().run();
        } finally {
            TootleCleanup();
        }
    }

    private void cleanup() {
        aiReleasePropertyStore(propertyStore);

        if (mesh != null) {
            par_shapes_free_mesh(meshOptimized);
            meshOptimized = null;

            par_shapes_free_mesh(mesh);
            mesh = null;
        }

        glDeleteProgram(programHUD);
        glDeleteProgram(programMesh);

        glDeleteBuffers(vboPerf);
        glDeleteBuffers(vboHUD);
        glDeleteBuffers(iboMesh);
        glDeleteBuffers(vboMesh);

        glDeleteVertexArrays(vao);

        glfwFreeCallbacks(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        if (debugCB != null) {
            debugCB.free();
        }
    }

    private void run() {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer gpuTimes = stack.callocFloat(3);

            while (!glfwWindowShouldClose(window)) {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                if (mesh != null) {
                    renderMesh(gpuTimes);
                }
                renderHUD();

                glfwSwapBuffers(window);
                glfwPollEvents();
            }
        } finally {
            cleanup();
        }
    }

    private static int buildShaderProgram(String vsh, String fsh) {
        int vshader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vshader, vsh);
        glCompileShader(vshader);
        if (glGetShaderi(vshader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetShaderInfoLog(vshader));
        }

        int fshader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fshader, fsh);
        glCompileShader(fshader);
        if (glGetShaderi(fshader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetShaderInfoLog(fshader));
        }

        int program = glCreateProgram();
        glAttachShader(program, vshader);
        glAttachShader(program, fshader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException(glGetProgramInfoLog(program));
        }

        glDeleteShader(fshader);
        glDeleteShader(vshader);

        return program;
    }

    private void updateViewport(int width, int height) {
        glViewport(0, 0, width, height);

        double fovY  = 45.0;
        double zNear = 0.01;
        double zFar  = 100.0;

        double aspect = (double)width / (double)height;

        double fH = tan(fovY / 360 * PI) * zNear;
        double fW = fH * aspect;

        projectionMatrix
            .setFrustum(-fW, fW, -fH, fH, zNear, zFar);

        modelViewProjectionMatrixHUD
            .setOrtho(0.0, width, height, 0.0, -1.0, 1.0)
            .translate(4.0, 4.0, 0.0)
            .scale(2.0, 2.0, 1.0);
    }

    private void renderMesh(FloatBuffer gpuTimes) {
        if (!fragmentShader) {
            if (Platform.get() != Platform.MACOSX) {
                glEnable(GL_RASTERIZER_DISCARD); // slower on macOS
            } else {
                glViewport(0, 0, 0, 0);
                glColorMask(false, false, false, false);
                glDepthMask(false);
            }
        }

        glUseProgram(programMesh);

        try (MemoryStack frame = stackPush()) {
            FloatBuffer uniform = frame.mallocFloat(4 * 4);

            glUniformMatrix4fv(uniformModelViewProjectionMatrix, false,
                projectionMatrix
                    .mul(modelViewMatrix, modelViewProjectionMatrix)
                    .get(uniform));

            glUniformMatrix3fv(uniformNormalMatrix, false,
                normalMatrix
                    .set(modelViewMatrix)
                    .transpose()
                    .invert()
                    .get(uniform));
        }
        glUniform1i(uniformCubeSize, cubeSize);

        glBindBuffer(GL_ARRAY_BUFFER, vboMesh);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        if (hasNormals) {
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, mesh.npoints() * 3 * 4);
        } else {
            setDefaultNormal();
        }

        gpuTimer.startGPUTimer();

        glDrawElementsInstanced(GL_TRIANGLES, mesh.ntriangles() * 3, GL_UNSIGNED_INT, 0, cubeSize * cubeSize * cubeSize);

        if (hasNormals) {
            glDisableVertexAttribArray(1);
        }

        if (wireframe) {
            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glPolygonOffset(-1, -1);

            if (hasNormals) {
                setDefaultNormal();
            }
            glDrawElementsInstanced(GL_TRIANGLES, mesh.ntriangles() * 3, GL_UNSIGNED_INT, 0, cubeSize * cubeSize * cubeSize);

            glPolygonOffset(0.0f, 0.0f);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glDisable(GL_POLYGON_OFFSET_LINE);
        }

        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        if (!fragmentShader) {
            if (Platform.get() != Platform.MACOSX) {
                glDisable(GL_RASTERIZER_DISCARD);
            } else {
                glDepthMask(true);
                glColorMask(true, true, true, true);
                glViewport(0, 0, width, height);
            }
        }

        glUseProgram(0);

        // We may get multiple results.
        int n = gpuTimer.stopGPUTimer(gpuTimes, 3);
        for (int i = 0; i < n; i++) {
            gpuGraph.update(gpuTimes.get(i));
        }
    }

    private void setDefaultNormal() {
        Vector3f v = new Matrix3d()
            .set(modelViewMatrix)
            .transpose()
            .transform(new Vector3f(0.0f, 1.0f, 0.0f))
            .normalize();

        glVertexAttrib3f(1, v.x, v.y, v.z);
    }

    private void renderHUD() {
        glUseProgram(programHUD);

        try (MemoryStack frame = stackPush()) {
            glUniformMatrix4fv(uniformModelViewProjectionMatrixHUD, false, modelViewProjectionMatrixHUD.get(frame.mallocFloat(4 * 4)));
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboHUD);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4 * 4, 3 * 4);

        glDrawArrays(GL_TRIANGLES, 0, vboHUDVertexCount);

        glDisableVertexAttribArray(1);

        float avg = gpuGraph.getAverage();
        if (avg != 0.0f) {
            try (MemoryStack frame = stackPush()) {
                ByteBuffer text  = frame.malloc(4 * 1024);
                int        quads = stb_easy_font_print((width - 100) / 2, 8, String.format("%.2f ms", avg * 1000.0f), null, text);
                text.limit(quads * 4 * 16);

                ByteBuffer triangles = frame.malloc(quads * 6 * 16);
                copyQuadsToTriangles(text, triangles);

                glBindBuffer(GL_ARRAY_BUFFER, vboPerf);
                glBufferSubData(GL_ARRAY_BUFFER, 0, triangles);

                glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
                glVertexAttrib4f(1, 1.0f, 1.0f, 1.0f, 1.0f);
                glDrawArrays(GL_TRIANGLES, 0, quads * 6);
            }
        }

        glDisableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glUseProgram(0);
    }

    private void setCubeSize(int size) {
        cubeSize = size;

        modelViewMatrix
            .translation(0.0, 0.0, -cubeSize * 2)
            .rotateX(toRadians(45.0))
            .rotateY(toRadians(45.0));

        updateHUD();
    }

    private void updateMesh() {
        if (meshKey != -1) {
            updateMesh(meshKey);
        }
    }

    private void updateMesh(int key) {
        meshKey = key;

        ParShapesMesh mesh = null;
        switch (key) {
            case GLFW_KEY_1:
                mesh = par_shapes_create_parametric_sphere(slices, stacks);
                break;
            case GLFW_KEY_2:
                mesh = par_shapes_create_hemisphere(slices, stacks);
                break;
            case GLFW_KEY_3:
                mesh = par_shapes_create_cylinder(slices, stacks);
                break;
            case GLFW_KEY_4:
                mesh = par_shapes_create_torus(slices, stacks, 0.25f);
                break;
            case GLFW_KEY_5:
                mesh = par_shapes_create_trefoil_knot(slices, stacks, 1.0f);
                break;
            case GLFW_KEY_6:
                mesh = par_shapes_create_klein_bottle(slices, stacks);
                if (mesh != null) {
                    par_shapes_scale(mesh, 0.1f, 0.1f, 0.1f);
                }
                break;
            case GLFW_KEY_7:
                String program =
                    " sx 2 sy 2" +
                    " ry 90 rx 90" +
                    " shape tube rx 15  call rlimb rx -15" +
                    " shape tube rx -15 call llimb rx 15" +
                    " shape tube ry 15  call rlimb ry -15" +
                    " shape tube ry 15  call llimb ry -15" +
                    " rule rlimb" +
                    "     sx 0.925 sy 0.925 tz 1 rx 1.2" +
                    "     call rlimb2" +
                    " rule rlimb2.1" +
                    "     shape connect" +
                    "     call rlimb" +
                    " rule rlimb2.1" +
                    "     rx 15  shape tube call rlimb rx -15" +
                    "     rx -15 shape tube call llimb rx 15" +
                    " rule rlimb.1" +
                    "     call llimb" +
                    " rule llimb.1" +
                    "     call rlimb" +
                    " rule llimb.10" +
                    "     sx 0.925 sy 0.925" +
                    "     tz 1" +
                    "     rx -1.2" +
                    "     shape connect" +
                    "     call llimb";

                mesh = par_shapes_create_lsystem(program, slices, 60);
                if (mesh != null) {
                    par_shapes_scale(mesh, 0.05f, 0.05f, 0.05f);
                }
                break;
            case GLFW_KEY_8:
                mesh = par_shapes_create_rock(seed, subdivisions);
                break;
        }

        if (mesh != null) {
            par_shapes_scale(mesh, 0.4f, 0.4f, 0.4f);
            setMesh(mesh);
        }
    }

    private static ParShapesMesh replaceMesh(ParShapesMesh oldMesh, ParShapesMesh newMesh) {
        if (oldMesh != null && oldMesh != newMesh) {
            par_shapes_free_mesh(oldMesh);
        }
        return newMesh;
    }

    private void setMesh(ParShapesMesh newMesh) {
        par_shapes_clone(
            mesh = replaceMesh(mesh, newMesh),
            meshOptimized = replaceMesh(meshOptimized, par_shapes_create_empty())
        );

        optimize(meshOptimized);
        updateMeshGPU();
        updateHUD();
    }

    private void updateMeshGPU() {
        ParShapesMesh mesh = optimized ? meshOptimized : this.mesh;

        int vc = mesh.npoints();

        glBindBuffer(GL_ARRAY_BUFFER, vboMesh);
        glBufferData(GL_ARRAY_BUFFER, vc * (3 + 3 + 2) * 4, GL_STATIC_DRAW);

        glBufferSubData(GL_ARRAY_BUFFER, vc * (0 + 0) * 4, mesh.points(vc * 3));

        hasNormals = memGetAddress(mesh.address() + ParShapesMesh.NORMALS) != NULL;
        if (hasNormals) {
            glBufferSubData(GL_ARRAY_BUFFER, vc * (3 + 0) * 4, mesh.normals(vc * 3));
        }

        int tc = mesh.ntriangles();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboMesh);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh.triangles(tc * 3), GL_STATIC_DRAW);
    }

    private void updateHUD() {
        ByteBuffer color  = memAlloc(4);
        ByteBuffer buffer = memAlloc(1024 * 128);

        setColor(color, 255, 255, 255, 0);
        String[] meshes = {
            "Sphere",
            "Hemisphere",
            "Cylinder",
            "Torus",
            "Trefoil knot",
            "Klein bottle",
            "l-system",
            "Rock"
        };

        for (int i = 0; i < meshes.length; i++) {
            if (i == meshKey - GLFW_KEY_1) {
                setColor(color, 255, 0, 255, 255);
            } else {
                setColor(color, 255, 255, 255, 255);
            }

            print(0, i * 10, "(" + (i + 1) + ") " + meshes[i], color, buffer);
        }
        if (meshKey == -1) {
            setColor(color, 255, 0, 255, 255);
        } else {
            setColor(color, 255, 255, 255, 255);
        }
        int i = meshes.length;
        print(0, i * 10, "(Ctrl+O) Custom", color, buffer);
        i += 2;

        if (mesh != null) {
            setColor(color, 255, 255, 255, 255);
            print(4, i * 10, "Vertices: " + mesh.npoints(), color, buffer);
            i++;
            print(0, i * 10, "Triangles: " + mesh.ntriangles(), color, buffer);
        }

        String[] controls = {
            "(F1-3) VCache Opt:",
            "(-/+) VCache Size:",
            "(F4) VGeom Opt:",
            "(O) Optimized:",
            "(W) Wireframe:",
            "(F) Fragment Shader:",
            "(up/down) Slices:",
            "(left/right) Stacks:",
            "(page up/down) Seed:",
            "(page up/down) Subdivisions:",
            "(CTRL -/+) Instances:"
        };

        int alignment = stb_easy_font_width(controls[controls.length - 1]);

        int y = height / 2 - controls.length * 10 - 4;

        setColor(color, 255, 255, 0, 255);

        i = 0;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + (vcacheMethod + 1), color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + vcacheSize, color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + (optimizeVertexMemory ? "ON" : "OFF"), color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + (optimized ? "ON" : "OFF"), color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + (wireframe ? "ON" : "OFF"), color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + (fragmentShader ? "ON" : "OFF"), color, buffer);
        i++;

        if (meshKey == -1 || meshKey == GLFW_KEY_8) {
            setColor(color, 64, 64, 0, 255);
        }
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + slices, color, buffer);
        i++;
        if (meshKey == -1 || meshKey == GLFW_KEY_7) {
            setColor(color, 64, 64, 0, 255);
        }
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + stacks, color, buffer);
        i++;

        if (meshKey == GLFW_KEY_8) {
            setColor(color, 255, 255, 0, 255);
        } else {
            setColor(color, 64, 64, 0, 255);
        }
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + seed, color, buffer);
        i++;
        y = print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + subdivisions, color, buffer);
        i++;
        setColor(color, 255, 255, 0, 255);
        print(alignment - stb_easy_font_width(controls[i]), y, controls[i] + " " + cubeSize + "^3", color, buffer);

        if (mesh == null) {
            String msg = "Error in mesh generation!";

            setColor(color, 255, 0, 0, 255);
            print(
                (width / 2 - stb_easy_font_width(msg)) / 2,
                (height / 2 - 5) / 2,
                msg, color, buffer
            );
        } else {
            String msg = "ACMR: " + (optimized ? acmrOptimized : acmr);
            print((width / 2) - stb_easy_font_width(msg) - 2, 20, msg, color, buffer);

            msg = "OVDR: " + (optimized ? ovdrOptimized : ovdr);
            print((width / 2) - stb_easy_font_width(msg) - 2, 30, msg, color, buffer);
        }

        buffer.flip();

        // convert to triangles
        ByteBuffer triangles = copyQuadsToTriangles(buffer, null);
        try {
            vboHUDVertexCount = triangles.limit() >> 4;

            glBindBuffer(GL_ARRAY_BUFFER, vboHUD);
            glBufferData(GL_ARRAY_BUFFER, triangles, GL_STATIC_DRAW);
        } finally {
            memFree(triangles);
        }

        memFree(color);
        memFree(buffer);
    }

    private static void setColor(ByteBuffer color, int r, int g, int b, int a) {
        color
            .put(0, (byte)r)
            .put(1, (byte)g)
            .put(2, (byte)b)
            .put(3, (byte)a);
    }

    private static int print(int x, int y, String text, ByteBuffer color, ByteBuffer buffer) {
        int quads = stb_easy_font_print(x, y, text, color, buffer);
        buffer.position(buffer.position() + quads * (4 * 4 * 4));
        return y + 10;
    }

    private static ByteBuffer copyQuadsToTriangles(ByteBuffer quads, ByteBuffer triangles) {
        int vertexCount = quads.remaining() >> 4;
        int quadCount   = vertexCount >> 2;

        if (triangles == null) {
            triangles = memAlloc(quadCount * 6 * 16);
        }

        long s = memAddress(quads);
        long t = memAddress(triangles);

        for (int i = 0; i < quadCount; i++) {
            long quad = s + i * (4 * 16);

            long triangle = t + i * (6 * 16);

            memCopy(quad + 0 * 16, triangle + 0 * 16, 16);
            memCopy(quad + 2 * 16, triangle + 1 * 16, 16);
            memCopy(quad + 1 * 16, triangle + 2 * 16, 16);

            memCopy(quad + 0 * 16, triangle + 3 * 16, 16);
            memCopy(quad + 3 * 16, triangle + 4 * 16, 16);
            memCopy(quad + 2 * 16, triangle + 5 * 16, 16);
        }

        return triangles;
    }

    private void importMesh() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);

            int result = NFD_OpenDialog(null, Paths.get("modules/core/src/test/resources/demo").toString(), pp);
            switch (result) {
                case NFD_OKAY:
                    AIScene scene = aiImportFileExWithProperties(pp.getStringUTF8(0),
                        aiProcess_JoinIdenticalVertices |
                        aiProcess_Triangulate |
                        aiProcess_GenSmoothNormals |
                        aiProcess_PreTransformVertices,
                        null,
                        propertyStore
                    );
                    nNFDi_Free(pp.get(0));

                    if (scene != null) {
                        try {
                            PointerBuffer meshes = scene.mMeshes();
                            for (int i = 0; i < meshes.remaining(); i++) {
                                AIMesh aiMesh = AIMesh.create(meshes.get(i));
                                try {
                                    ParShapesMesh mesh = copyAssimpToParShapes(aiMesh);
                                    par_shapes_scale(mesh, 0.4f, 0.4f, 0.4f);
                                    setMesh(mesh);
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } finally {
                            aiFreeScene(scene);
                        }
                    } else {
                        System.err.println("Failed to load scene");
                    }
                    break;
                case NFD_ERROR:
                    System.err.format("Error: %s\n", NFD_GetError());
            }
        }
    }

    private static ParShapesMesh copyAssimpToParShapes(AIMesh aiMesh) {
        ParShapesMesh mesh = par_shapes_create_empty();

        memPutInt(mesh.address() + ParShapesMesh.NPOINTS, aiMesh.mNumVertices());
        memPutInt(mesh.address() + ParShapesMesh.NTRIANGLES, aiMesh.mNumFaces());

        memPutAddress(mesh.address() + ParShapesMesh.POINTS, memAddress(copyAssimpToParShapes(aiMesh.mVertices())));
        if (aiMesh.mNormals() != null) {
            memPutAddress(mesh.address() + ParShapesMesh.NORMALS, memAddress(copyAssimpToParShapes(aiMesh.mNormals())));
        }

        AIFace.Buffer faces = aiMesh.mFaces();

        IntBuffer triangles = memAllocInt(faces.remaining() * 3);
        for (AIFace face : faces) {
            IntBuffer indices = face.mIndices();
            if (indices.remaining() != 3) {
                throw new IllegalStateException();
            }
            triangles.put(indices.get(0));
            triangles.put(indices.get(1));
            triangles.put(indices.get(2));
        }
        triangles.flip();

        memPutAddress(mesh.address() + ParShapesMesh.TRIANGLES, memAddress(triangles));

        return mesh;
    }

    private static FloatBuffer copyAssimpToParShapes(AIVector3D.Buffer ai) {
        FloatBuffer ps = memAllocFloat(ai.remaining() * 3);
        for (int i = 0; i < ai.remaining(); i++) {
            AIVector3D v = ai.get(i);

            int p = i * 3;
            ps.put(p + 0, v.x());
            ps.put(p + 1, v.y());
            ps.put(p + 2, v.z());
        }
        return ps;
    }

    private void shuffleMesh() {
        int ntriangles = mesh.ntriangles();

        int[] order = new int[ntriangles];
        for (int i = 0; i < ntriangles; i++) {
            order[i] = i;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = order.length - 1; 0 < i; i--) {
            int index = random.nextInt(i + 1);
            int tmp   = order[index];
            order[index] = order[i];
            order[i] = tmp;
        }

        IntBuffer triangles = mesh.triangles(mesh.ntriangles() * 3);
        IntBuffer shuffled  = memAllocInt(triangles.remaining());
        try {
            for (int i = 0; i < ntriangles; i++) {
                int src = i * 3;
                int trg = order[i] * 3;

                shuffled.put(trg + 0, triangles.get(src + 0));
                shuffled.put(trg + 1, triangles.get(src + 1));
                shuffled.put(trg + 2, triangles.get(src + 2));
            }
            triangles.put(shuffled);
            triangles.flip();
            shuffled.flip();
        } finally {
            memFree(shuffled);
        }

        printStats("BEFORE", mesh.points(mesh.npoints() * 3), triangles);
        if (!optimized) {
            updateMeshGPU();
        }
        updateHUD();
    }

    private void optimize(ParShapesMesh mesh) {
        int nVertices = mesh.npoints();
        int nFaces    = mesh.ntriangles();

        FloatBuffer pVB  = mesh.points(nVertices * 3);
        IntBuffer   pnIB = mesh.triangles(nFaces * 3);

        if (Platform.get() != Platform.WINDOWS) {
            System.out.println("\n(overdraw calculation may take several seconds, please be patient)");
        }

        /*
        BEFORE OPTIMIZATION
        -------------------
        ACMR: 1.03125
        Overdraw AVG:0.26592624
        Overdraw MAX:3.156979
         */
        printStats("BEFORE", pVB, pnIB);

        IntBuffer pnIBOut = optimizeVertexMemory ?
            memAllocInt(pnIB.remaining()) :
            pnIB;

        try {
            System.out.println("\nOptimizing...");
            long t = System.nanoTime();
            int  err;
            switch (vcacheMethod) {
                case 1:
                    err = optimize(pVB, pnIB, pnIBOut);
                    break;
                case 2:
                    err = optimizeFast(pVB, pnIB, pnIBOut);
                    break;
                default:
                    err = optimizeVCacheOnly(pVB, pnIB, pnIBOut);
            }
            if (err != TOOTLE_OK) {
                throw new IllegalStateException("Failed: " + err);
            }
            t = System.nanoTime() - t;
            System.out.println("DONE in " + (t / 1_000_000L) + "ms");

            if (optimizeVertexMemory) {
                System.out.println("\nRearranging...");
                t = System.nanoTime();
                FloatBuffer buffer        = memAllocFloat(pVB.remaining());
                IntBuffer   pnVertexRemap = memAllocInt(nVertices);
                try {
                    err = TootleOptimizeVertexMemory(
                        pVB,
                        pnIBOut,
                        3 * Float.BYTES,
                        buffer,
                        pnIB,
                        pnVertexRemap
                    );
                    if (err != TOOTLE_OK) {
                        throw new IllegalStateException("Failed: " + err);
                    }
                    pVB.put(buffer);
                    pVB.flip();

                    FloatBuffer normals = mesh.normals(nVertices * 3);
                    if (normals != null) {
                        buffer.flip();
                        for (int i = 0; i < nVertices; i++) {
                            int n = pnVertexRemap.get(i) * 3;
                            buffer.put(n + 0, normals.get());
                            buffer.put(n + 1, normals.get());
                            buffer.put(n + 2, normals.get());
                        }
                        normals.flip();
                        normals.put(buffer);
                    }
                } finally {
                    memFree(pnVertexRemap);
                    buffer.clear();
                    memFree(buffer);
                }
                t = System.nanoTime() - t;
                System.out.println("DONE in " + (t / 1_000L) + "us");
            }

            printStats("AFTER", pVB, pnIB);
        } finally {
            if (optimizeVertexMemory) {
                memFree(pnIBOut);
            }
        }
    }

    /*
    vertex cache optimization only
    809ms

    ACMR: 0.64476764
    Overdraw AVG:0.26596618
    Overdraw MAX:3.1577697
     */
    private int optimizeVCacheOnly(FloatBuffer pVB, IntBuffer pnIB, IntBuffer pnIBOut) {
        return TootleOptimizeVCache(
            pnIB,
            pVB.remaining() / 3,
            vcacheSize,
            pnIBOut,
            null,
            TOOTLE_VCACHE_AUTO
        );
    }

    /*
    slow optimizer (TootleClusterMesh -> TootleVCacheClusters -> TootleOptimizeOverdraw)

    TOOTLE_OVERDRAW_AUTO:
    ---------------------
    12841ms

    ACMR: 0.6474919
    Overdraw AVG:0.25006402
    Overdraw MAX:1.7714512

    TOOTLE_OVERDRAW_FAST:
    ---------------------
    319ms

    ACMR: 0.6474919
    Overdraw AVG:0.24489474
    Overdraw MAX:1.489917
    */
    private int optimize(FloatBuffer pVB, IntBuffer pnIB, IntBuffer pnIBOut) {
        return TootleOptimize(
            pVB,
            pnIB,
            3 * Float.BYTES,
            vcacheSize,
            null,
            TOOTLE_CCW,
            pnIBOut,
            null,
            TOOTLE_VCACHE_AUTO,
            TOOTLE_OVERDRAW_AUTO
        );
    }

    /*
    fast optimizer (TootleFastOptimizeVCacheAndClusterMesh -> TootleOptimizeOverdraw)
    9ms

    ACMR: 0.84388506
    Overdraw AVG:0.14286888
    Overdraw MAX:0.72162914
     */
    private int optimizeFast(FloatBuffer pVB, IntBuffer pnIB, IntBuffer pnIBOut) {
        return TootleFastOptimize(
            pVB,
            pnIB,
            3 * Float.BYTES,
            vcacheSize,
            TOOTLE_CCW,
            pnIBOut,
            null,
            //  low value: prefer lower ACMR, higher overdraw
            // high value: prefer higher ACMR, lower overdraw
            0.0f /*TOOTLE_DEFAULT_ALPHA*/
        );
    }

    private void printStats(
        String title,
        FloatBuffer pVB,
        IntBuffer pnIB
    ) {
        System.out.println();
        System.out.println(title);

        try (MemoryStack stack = stackPush()) {
            FloatBuffer pfA = stack.mallocFloat(1);
            FloatBuffer pfB = stack.mallocFloat(1);

            int err = TootleMeasureCacheEfficiency(pnIB, vcacheSize, pfA);
            if (err != TOOTLE_OK) {
                throw new IllegalStateException("Failed: " + err);
            }
            System.out.println("\tACMR: " + pfA.get(0));
            if ("BEFORE".equals(title)) {
                acmr = pfA.get(0);
            } else {
                acmrOptimized = pfA.get(0);
            }

            if (Platform.get() == Platform.WINDOWS || vcacheMethod != 0) {
                err = TootleMeasureOverdraw(
                    pVB,
                    pnIB,
                    3 * Float.BYTES,
                    null,
                    TOOTLE_CCW,
                    pfA,
                    pfB,
                    TOOTLE_OVERDRAW_AUTO
                );
                if (err != TOOTLE_OK) {
                    throw new IllegalStateException("Failed: " + err);
                }
                float avg = max(0.0f, pfA.get(0));
                System.out.println("\tOverdraw AVG: " + avg);
                System.out.println("\tOverdraw MAX: " + pfB.get(0));
                if ("BEFORE".equals(title)) {
                    ovdr = avg;
                } else {
                    ovdrOptimized = avg;
                }
            } else {
                ovdr = ovdrOptimized = -1.0f;
            }
        }
    }

    // From NanoVG ExampleGL3

    private static final int GRAPH_HISTORY_COUNT = 100;
    private static final int GPU_QUERY_COUNT     = 5;

    static class PerfGraph {
        float[] values = new float[GRAPH_HISTORY_COUNT];
        int head;

        void update(float frameTime) {
            head = (head + 1) % GRAPH_HISTORY_COUNT;
            values[head] = frameTime;
        }

        float getAverage() {
            float avg = 0;
            for (int i = 0; i < GRAPH_HISTORY_COUNT; i++) {
                avg += values[i];
            }
            return avg / (float)GRAPH_HISTORY_COUNT;
        }
    }

    static class GPUTimer {
        final boolean supported;

        final IntBuffer queries;

        int cur,
            ret;

        GPUTimer() {
            supported = GL.getCapabilities().GL_ARB_timer_query;
            if (supported) {
                queries = BufferUtils.createIntBuffer(GPU_QUERY_COUNT);
                glGenQueries(queries);
            } else {
                queries = null;
            }
        }

        void startGPUTimer() {
            if (!supported) {
                return;
            }
            glBeginQuery(GL_TIME_ELAPSED, queries.get(cur % GPU_QUERY_COUNT));
            cur++;
        }

        int stopGPUTimer(FloatBuffer times, int maxTimes) {
            if (!supported) {
                return 0;
            }

            glEndQuery(GL_TIME_ELAPSED);

            int n = 0;
            try (MemoryStack stack = stackPush()) {
                IntBuffer available = stack.ints(1);
                while (available.get(0) != 0 && ret <= cur) {
                    // check for results if there are any
                    glGetQueryObjectiv(queries.get(ret % GPU_QUERY_COUNT), GL_QUERY_RESULT_AVAILABLE, available);
                    if (available.get(0) != 0) {
                        LongBuffer timeElapsed = stack.mallocLong(1);
                        glGetQueryObjectui64v(queries.get(ret % GPU_QUERY_COUNT), GL_QUERY_RESULT, timeElapsed);
                        ret++;
                        if (n < maxTimes) {
                            times.put(n, (float)((double)timeElapsed.get(0) * 1e-9));
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }

}