/*
 * JNIDirect provides runtime JNI native method generation.
 *
 * Copyright (C) 2002-2005 by Patrick C. Beard <pcbeard@mac.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

//
//  Linker.java
//
//  Created by Patrick Beard on Thu Dec 19 2002.
//

package jnidirect;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

/**
 * Convenience class to group all of the JNIDirect constants together.
 */
abstract class Constants {
    static final HashMap jniTypes, marshalledTypes, overloadSignatures, jarrayAccessors, jarrayReleasers, jmethodCallers;
    static {
        jniTypes = new HashMap();
        jniTypes.put(Void.TYPE, "void");
        jniTypes.put(Boolean.TYPE, "jboolean");
        jniTypes.put(Byte.TYPE, "jbyte");
        jniTypes.put(Short.TYPE, "jshort");
        jniTypes.put(Character.TYPE, "jchar");
        jniTypes.put(Integer.TYPE, "jint");
        jniTypes.put(Long.TYPE, "jlong");
        jniTypes.put(Float.TYPE, "jfloat");
        jniTypes.put(Double.TYPE, "jdouble");
        jniTypes.put(Method.class, "jobject");
        jniTypes.put(Object.class, "jobject");
        jniTypes.put(Class.class, "jclass");
        jniTypes.put(boolean[].class, "jbooleanArray");
        jniTypes.put(byte[].class, "jbyteArray");
        jniTypes.put(char[].class, "jcharArray");
        jniTypes.put(short[].class, "jshortArray");
        jniTypes.put(int[].class, "jintArray");
        jniTypes.put(long[].class, "jlongArray");
        jniTypes.put(float[].class, "jfloatArray");
        jniTypes.put(double[].class, "jdoubleArray");
        marshalledTypes = new HashMap();
        marshalledTypes.put(Void.TYPE, "void");
        marshalledTypes.put(Boolean.TYPE, "jboolean");
        marshalledTypes.put(Byte.TYPE, "jbyte");
        marshalledTypes.put(Character.TYPE, "jchar");
        marshalledTypes.put(Short.TYPE, "jshort");
        marshalledTypes.put(Integer.TYPE, "jint");
        marshalledTypes.put(Long.TYPE, "jlong");
        marshalledTypes.put(Float.TYPE, "jfloat");
        marshalledTypes.put(Double.TYPE, "jdouble");
        marshalledTypes.put(boolean[].class, "jboolean*");
        marshalledTypes.put(byte[].class, "jbyte*");
        marshalledTypes.put(char[].class, "jchar*");
        marshalledTypes.put(short[].class, "jshort*");
        marshalledTypes.put(int[].class, "jint*");
        marshalledTypes.put(long[].class, "jlong*");
        marshalledTypes.put(float[].class, "jfloat*");
        marshalledTypes.put(double[].class, "jdouble*");
        overloadSignatures = new HashMap();
        overloadSignatures.put(Boolean.TYPE, "Z");
        overloadSignatures.put(Byte.TYPE, "B");
        overloadSignatures.put(Character.TYPE, "C");
        overloadSignatures.put(Short.TYPE, "S");
        overloadSignatures.put(Integer.TYPE, "I");
        overloadSignatures.put(Long.TYPE, "J");
        overloadSignatures.put(Float.TYPE, "F");
        overloadSignatures.put(Double.TYPE, "D");
        overloadSignatures.put(Object.class, "Ljava_lang_Object_2");
        overloadSignatures.put(Class.class, "Ljava_lang_Class_2");
        overloadSignatures.put(boolean[].class, "_3Z");
        overloadSignatures.put(byte[].class, "_3B");
        overloadSignatures.put(char[].class, "_3C");
        overloadSignatures.put(short[].class, "_3S");
        overloadSignatures.put(int[].class, "_3I");
        overloadSignatures.put(long[].class, "_3J");
        overloadSignatures.put(float[].class, "_3F");
        overloadSignatures.put(double[].class, "_3D");
        jarrayAccessors = new HashMap();
        jarrayAccessors.put(boolean[].class, "GetBooleanArrayElements");
        jarrayAccessors.put(byte[].class, "GetByteArrayElements");
        jarrayAccessors.put(char[].class, "GetCharArrayElements");
        jarrayAccessors.put(short[].class, "GetShortArrayElements");
        jarrayAccessors.put(int[].class, "GetIntArrayElements");
        jarrayAccessors.put(long[].class, "GetLongArrayElements");
        jarrayAccessors.put(float[].class, "GetFloatArrayElements");
        jarrayAccessors.put(double[].class, "GetDoubleArrayElements");
        jarrayReleasers = new HashMap();
        jarrayReleasers.put(boolean[].class, "ReleaseBooleanArrayElements");
        jarrayReleasers.put(byte[].class, "ReleaseByteArrayElements");
        jarrayReleasers.put(char[].class, "ReleaseCharArrayElements");
        jarrayReleasers.put(short[].class, "ReleaseShortArrayElements");
        jarrayReleasers.put(int[].class, "ReleaseIntArrayElements");
        jarrayReleasers.put(long[].class, "ReleaseLongArrayElements");
        jarrayReleasers.put(float[].class, "ReleaseFloatArrayElements");
        jarrayReleasers.put(double[].class, "ReleaseDoubleArrayElements");
        jmethodCallers = new HashMap();
        jmethodCallers.put(Void.TYPE, "CallVoidMethod");
        jmethodCallers.put(Boolean.TYPE, "CallBooleanMethod");
        jmethodCallers.put(Byte.TYPE, "CallByteMethod");
        jmethodCallers.put(Character.TYPE, "CallCharMethod");
        jmethodCallers.put(Short.TYPE, "CallShortMethod");
        jmethodCallers.put(Integer.TYPE, "CallIntMethod");
        jmethodCallers.put(Long.TYPE, "CallLongMethod");
        jmethodCallers.put(Float.TYPE, "CallFloatMethod");
        jmethodCallers.put(Double.TYPE, "CallDoubleMethod");
    }

    static final HashMap jnidirectIntrinsics;
    static {
        jnidirectIntrinsics = new HashMap();
        jnidirectIntrinsics.put("$read$byte$", "  return *(jbyte*)arg0;");
        jnidirectIntrinsics.put("$read$char$", "  return *(jchar*)arg0;");
        jnidirectIntrinsics.put("$read$short$", "  return *(jshort*)arg0;");
        jnidirectIntrinsics.put("$read$int$", "  return *(jint*)arg0;");
        jnidirectIntrinsics.put("$read$long$", "  return *(jlong*)arg0;");
        jnidirectIntrinsics.put("$read$float$", "  return *(jfloat*)arg0;");
        jnidirectIntrinsics.put("$read$double$", "  return *(jdouble*)arg0;");
        jnidirectIntrinsics.put("$write$byte$", "  *(jbyte*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$char$", "  *(jchar*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$short$", "  *(jshort*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$int$", "  *(jint*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$long$", "  *(jlong*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$float$", "  *(jfloat*)arg0 = arg1;");
        jnidirectIntrinsics.put("$write$double$", "  *(jdouble*)arg0 = arg1;");
        jnidirectIntrinsics.put("$jni$FromReflectedMethod$", "  return (*env)->FromReflectedMethod(env, arg0);");
        jnidirectIntrinsics.put("$jni$NewWeakGlobalRef$", "  return (*env)->NewWeakGlobalRef(env, arg0);");
        jnidirectIntrinsics.put("$jni$DeleteWeakGlobalRef$", "  (*env)->DeleteWeakGlobalRef(env, arg0);");
        jnidirectIntrinsics.put("$jni$RegisterNatives$", "  return (*env)->RegisterNatives(env, arg0, (const JNINativeMethod *)arg1, arg2);");
        jnidirectIntrinsics.put("$jni$GetJavaVM$", "  JavaVM* jvm; (*env)->GetJavaVM(env, &jvm); return (jint)jvm;");
    }

    static final String kGlobalPrefix = "$global$";
    static final String kOffsetOfPrefix = "$offsetof$";

    static final boolean kCachingLibraries = Boolean.getBoolean("jnidirect.cache_libraries");
    static final boolean kDynamicSymbolLookup = Boolean.getBoolean("jnidirect.dynamic_symbols");
    static final boolean kKeepTemporaryFiles = Boolean.getBoolean("jnidirect.keep_temporary_files");
}

/**
 * Provides an implementation of Java native methods by generating JNI C code at runtime.
 * @author  Patrick C. Beard
 * @version 1.0, 12/19/02
 */
public abstract class Linker extends Constants {
    /**
     * Generates native methods for the specified class.
     *
     * @param      c   the class to generate native methods for.
     */
    public static void link(Class c) {
        link(c, null, null, false, null);
    }

    /**
     * Generates native methods for the specified class, linking against
     * 1 or more specified Mac OS X frameworks.
     *
     * @param      c            the class to generate native methods for.
     * @param      frameworks   array of names of frameworks to link against.
     */
    public static void link(Class c, String[] frameworks) {
        link(c, frameworks, null, false, null);
    }

    /**
     * Generates native methods for the specified class, also generating
     * method closure glue for the specified methods.
     *
     * @param      c         the class to generate native methods for.
     * @param      closures  array of methods to generate method closure glue for.
     */
    public static void link(Class c, Method[] closures) {
        link(c, null, closures, false, null);
    }

    /**
     * Generates native methods for the specified class, linking against
     * 1 or more specified Mac OS X frameworks, also generating method closure
     * glue for the specified methods.
     *
     * @param      c            the class to generate native methods for.
     * @param      frameworks   array of names of frameworks to link against.
     * @param      closures     array of methods to generate method closure glue for.
     */
    public static void link(Class c, String[] frameworks, Method[] closures) {
        link(c, frameworks, closures, false, null);
    }

    /**
     * Generates native methods for the specified class, linking against
     * 1 or more specified Mac OS X frameworks, also generating method closure
     * glue for the specified methods.
     *
     * @param      c            the class to generate native methods for.
     * @param      frameworks   array of names of frameworks to link against.
     * @param      closures     array of methods to generate method closure glue for.
     * @param      bundle       if true, use loadBundle() rather than System.load().
     */
    public static void link(Class c, String[] frameworks, Method[] closures, boolean bundle) {
        link(c, frameworks, closures, bundle, null);
    }

    /**
     * Generates native methods for the specified class, linking against
     * 1 or more specified Mac OS X frameworks, also generating method closure
     * glue for the specified methods.
     *
     * @param      c            the class to generate native methods for.
     * @param      frameworks   array of names of frameworks to link against.
     * @param      closures     array of methods to generate method closure glue for.
     * @param      bundle       if true, use loadBundle() rather than System.load().
     * @param      headers      array of header file specifications, e.g. <stdio.h> to include.
     */
    public static void link(Class c, String[] frameworks, Method[] closures, boolean bundle, String[] headers) {
        // 0. Try to load the glue library from a resource, if possible.
        File glueLibrary = getLibraryResource(c);
        if (glueLibrary != null) {
            try {
                // this will throw an exception, if the same library is loaded twice.
                System.load(glueLibrary.getPath());
                return;
            } catch (UnsatisfiedLinkError err) {
            }
        }

        // 1. accumulate list of native methods.
        Method[] nativeMethods = getNativeMethods(c);

        // 2. Determine which methods are overloaded. These require a longer method name to disambiguate.
        HashMap overloads = getOverloads(nativeMethods);

        StringBuffer source = new StringBuffer();
        if (headers != null) {
            for (int i = 0; i < headers.length; ++i) {
                source.append("#include ").append(headers[i]).append("\n");
            }
        }
        if (kDynamicSymbolLookup) source.append("#include <dlfcn.h>\n");
        source.append("#include <stddef.h>\n"); // to get offsetof() macro.
        source.append("#include <jni.h>\n\n");
        
        // 3. generate C glue for each method. this requires examining the type of each method
        // parameter, and generating parameter marshalling code. The only parameter types we
        // support are simple scalar types, and arrays of same.
        int count = nativeMethods.length;
        for (int i = 0; i < count; ++i) {
            Method nativeMethod = nativeMethods[i];
            generateGlueSource(c, nativeMethod, overloads.get(nativeMethod.getName()) == Boolean.TRUE, source);
        }

        // 4. generate glue for method closures. Use JNI_OnLoad() hook to initialize a table of closure
        // function pointers that gets passed back into the Linker.
        if (closures != null) {
            generateMethodClosureGlue(c, closures, bundle, source);
        }

        // 5. generate JNIDirect_LoadBundle()/JNIDirect_UnloadBundle() functions which
        // are called if the native library is a dynamically loadable bundle.
        if (bundle) {
            generateBundle(c, nativeMethods, overloads, source);
        }

        // 6. run C compiler to generate a dynamic library that implements the native methods.
        glueLibrary = generateGlueLibrary(c, source.toString(), frameworks);
        if (glueLibrary != null) {
            if (bundle) {
                loadBundle(c, nativeMethods, glueLibrary);
            } else {
                System.load(glueLibrary.getPath());
            }
        }

        // 7. optionally store the generated library in the same location as the class file.
        storeLibraryResource(c, glueLibrary);
    }
    
    /**
     * Generates native methods for the specified class, which is assumed to
     * model a C struct. The only valid native methods for this type of class
     * are of the form $sizeof()$ and $offsetof$fieldName, and other intrinsic
     * methods supported by JNIDirect.
     *
     * @param      c            the class to generate native methods for.
     * @param      headers      array of header file specifications, e.g. <stdio.h> to include.
     */
    public static void struct(Class c, String[] headers) {
        link(c, null, null, false, headers);
    }

    /**
     * Checks to see if a class.getName() + ".jnilib" exists as a resource. If it does,
     * and it is in a .jar file, the resource is copied into /tmp, and a reference
     * to it is returned as a java.io.File object. Otherwise if the library is already
     * in the file system, the java.io.File is returned directly.
     */
    private static File getLibraryResource(Class c) {
        if (!kCachingLibraries)
            return null;
        try {
            String className = c.getName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0)
                className = className.substring(lastDot + 1);
            java.net.URL resource = c.getResource(className + ".jnilib");
            if (resource != null) {
                // if already a file, load it directly.
                if (resource.getProtocol().equals("file"))
                    return new File(resource.getPath());
                InputStream input = resource.openStream();
                if (input != null) {
                    // copy this stream to a temporary disk file, and load it from there.
                    String classId = mangle(c.getName());
                    File libraryFile = File.createTempFile("jnidirect_" + classId + "_", ".jnilib");
                    libraryFile.deleteOnExit();
                    FileOutputStream output = new FileOutputStream(libraryFile);
                    byte[] buffer = new byte[8192];
                    int count = input.read(buffer);
                    while (count > 0) {
                        output.write(buffer, 0, count);
                        count = input.read(buffer);
                    }
                    output.close();
                    input.close();
                    return libraryFile;
                }
            }
        } catch (IOException ioex) {
        }
        return null;
    }

    /**
     * Only do this during "deployment" mode.
     */
    private static void storeLibraryResource(Class c, File libraryFile) {
        if (!kCachingLibraries)
            return;
        String className = c.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0)
            className = className.substring(lastDot + 1);
        java.net.URL resource = c.getResource(className + ".class");
        if (resource.getProtocol().equals("file")) {
            // save the library file to the same directory as the class file itself.
            String path = resource.getPath();
            File resourceFile = new File(path.substring(0, path.length() - 5) + "jnilib");
            if (!libraryFile.renameTo(resourceFile)) {
                try {
                    // have to copy the file.
                    FileInputStream input = new FileInputStream(libraryFile);
                    FileOutputStream output = new FileOutputStream(resourceFile);
                    byte[] buffer = new byte[8192];
                    int count = input.read(buffer);
                    while (count > 0) {
                        output.write(buffer, 0, count);
                        count = input.read(buffer);
                    }
                    input.close();
                    output.close();
                } catch (IOException ioex) {
                }
            }
        }
    }

    /**
     * Returns all declared native methods of specified class.
     * @param c a class
     * @return  nativeMethods  list of native methods declared by the class.
     */
    private static Method[] getNativeMethods(Class c) {
        Method[] declaredMethods = c.getDeclaredMethods();
        ArrayList list = new ArrayList();
        for (int i = declaredMethods.length - 1; i >= 0; --i) {
            Method m = declaredMethods[i];
            if ((m.getModifiers() & Modifier.NATIVE) != 0)
                list.add(m);
        }
        Method[] nativeMethods = new Method[list.size()];
        list.toArray(nativeMethods);
        return nativeMethods;
    }

    private static HashMap getOverloads(Method[] nativeMethods) {
        HashMap overloads = new HashMap();
        int count = nativeMethods.length;
        for (int i = 0; i < count; ++i) {
            String methodName = nativeMethods[i].getName();
            overloads.put(methodName, overloads.containsKey(methodName) ? Boolean.TRUE : Boolean.FALSE);
        }
        return overloads;
    }

    static String mangle(String name) {
        StringBuffer buffer = new StringBuffer();
        char[] chars = name.toCharArray();
        int count = chars.length;
        for (int i = 0; i < count; ++i) {
            char ch = chars[i];
            switch (chars[i]) {
            case '$':
                buffer.append("_00024");
                break;
            case '_':
                buffer.append("_1");
                break;
            case '.':
                buffer.append('_');
                break;
            default:
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    private static String getMethodIdentifier(Class c, Method m, boolean overloaded) {
        StringBuffer buffer = new StringBuffer("Java_");
        buffer.append(mangle(c.getName()));
        buffer.append("_");
        buffer.append(mangle(m.getName()));
        if (overloaded) {
            Class[] parameterTypes = m.getParameterTypes();
            int parameterCount = parameterTypes.length;
            buffer.append("__");
            for (int i = 0; i < parameterCount; ++i)
                buffer.append(overloadSignatures.get(parameterTypes[i]));
        }
        return buffer.toString();
    }
    
    private static String getClassStructName(Class c) {
        // returns the leaf name of the class, with '$' characters stripped out.
        String structName = c.getName();
        int lastDot = structName.lastIndexOf('.');
        if (lastDot >= 0)
            structName = structName.substring(lastDot + 1);
        return structName.replaceAll("\\$", "");
    }
    
    private static void generateGlueSource(Class c, Method m, boolean overloaded, StringBuffer source) {
        String methodName = m.getName();
        Class returnType = m.getReturnType();
        Class[] parameterTypes = m.getParameterTypes();
        int parameterCount = parameterTypes.length;

        // declare the JNI native method.
        String classId = mangle(c.getName());
        source.append("JNIEXPORT ").append(jniTypes.get(returnType)).append(" JNICALL Java_").append(classId).append("_").append(mangle(methodName));
        if (overloaded) {
            source.append("__");
            for (int i = 0; i < parameterCount; ++i)
                source.append(overloadSignatures.get(parameterTypes[i]));
        }
        source.append("(JNIEnv *env, jclass thisClass");
        for (int i = 0; i < parameterCount; ++i) {
            source.append(", ");
            source.append(jniTypes.get(parameterTypes[i]));
            source.append(" arg");
            source.append(i);
        }
        source.append(")\n");
        source.append("{\n");

        // is this method a JNIDirect intrinsic?
        if (methodName.charAt(0) == '$') {
            String intrinsic = (String) jnidirectIntrinsics.get(methodName);
            if (intrinsic != null) {
                source.append(intrinsic).append("\n}\n\n");
                return;
            }

            // is this method a global variable reference?
            if (methodName.startsWith(kGlobalPrefix)) {
                String globalName = methodName.substring(kGlobalPrefix.length(), methodName.length());
                source.append("  extern ").append(jniTypes.get(returnType)).append(" ").append(globalName).append(";\n");
                source.append("  return ").append(globalName).append(";\n}\n\n");
                return;
            }
            
            // is this method a special $sizeof$() for struct classes?
            if (methodName.equals("$sizeof$")) {
                source.append("  return sizeof(struct ").append(getClassStructName(c)).append(");\n}\n\n");
                return;
            }
            
            // if this method a special $offsetof$fieldName()for struct classes?
            if (methodName.startsWith(kOffsetOfPrefix)) {
                String fieldName = methodName.substring(kOffsetOfPrefix.length(), methodName.length());
                source.append("  return offsetof(struct ").append(getClassStructName(c)).append(", ").append(fieldName).append(");\n}\n\n");
                return;
            }
        }

        // declare local variable declarations.
        if (!returnType.equals(Void.TYPE))
            source.append("  ").append(jniTypes.get(returnType)).append(" rv;\n");
        boolean hasArrayParameters = false;
        for (int i = 0; i < parameterCount; ++i) {
            Class parameterType = parameterTypes[i];
            if (parameterType.isArray()) {
                source.append("  ").append(marshalledTypes.get(parameterType));
                source.append(" marg").append(i).append(";\n");
                hasArrayParameters = true;
            }
        }
        if (hasArrayParameters)
            source.append("  jboolean isCopy;\n");

        // is this a special function call primitive?
        boolean isCallMethod = methodName.equals("$call$");
        if (isCallMethod) {
            // declare the function pointer we are calling.
            // FIXME should declare all of the parameters as well.
            source.append("  ").append(jniTypes.get(returnType)).append(" (*f) () = (void*)arg0;\n");
        } else {
            if (kDynamicSymbolLookup) {
                // GCC 4 is much pickier, so always look up the function dynamically.
                // FIXME should limit the scope of the lookup to a particular library opened with
                // dlopen(). should only really do this for builtin functions.
                source.append("  static ").append(jniTypes.get(returnType)).append(" (*f) () = 0;\n");
                source.append("  if (!f) f = dlsym(RTLD_DEFAULT, \"").append(methodName).append("\");\n");
            } else {
                // GCC 3.3 is much more forgiving, so we can get away with mismatched declarations.
                source.append("  extern ").append(marshalledTypes.get(returnType));
                source.append(" ").append(methodName).append("(");
                if (!overloaded && parameterCount > 0) {
                    source.append(marshalledTypes.get(parameterTypes[0]));
                    for (int i = 1; i < parameterCount; ++i)
                        source.append(", ").append(marshalledTypes.get(parameterTypes[i]));
                }
                source.append(");\n");
            }
        }

        // marshall the array args.
        for (int i = 0; i < parameterCount; ++i) {
            Class parameterType = parameterTypes[i];
            if (parameterType.isArray()) {
                source.append("  marg").append(i).append(" = (arg").append(i);
                source.append(" ? (*env)->").append(jarrayAccessors.get(parameterType));
                source.append("(env, arg").append(i).append(", &isCopy) : NULL);\n");
            }
        }
        // XXX use isCopy in some way? would need isCopy[i] to keep each value live.

        // call the native method.
        source.append("  ");
        if (!returnType.equals(Void.TYPE))
            source.append("rv = ");
        if (isCallMethod) {
            source.append("(*f)(");
            if (parameterCount > 1) {
                source.append(parameterTypes[1].isArray() ? "marg1" : "arg1");
                for (int i = 2; i < parameterCount; ++i)
                    source.append(", ").append(parameterTypes[i].isArray() ? "marg" : "arg").append(i);
            }
        } else {
            if (kDynamicSymbolLookup)
                source.append("(*f)(");
            else
                source.append(methodName).append("(");
            if (parameterCount > 0) {
                source.append(parameterTypes[0].isArray() ? "marg0" : "arg0");
                for (int i = 1; i < parameterCount; ++i)
                    source.append(", ").append(parameterTypes[i].isArray() ? "marg" : "arg").append(i);
            }
        }
        source.append(");\n");

        // release all the marshalled arrays.
        for (int i = 0; i < parameterCount; ++i) {
            Class parameterType = parameterTypes[i];
            if (parameterType.isArray()) {
                source.append("  if (arg").append(i).append(") (*env)->").append(jarrayReleasers.get(parameterType));
                source.append("(env, arg").append(i).append(", marg").append(i).append(", JNI_COMMIT);\n");
            }
        }

        // return the result.
        if (!returnType.equals(Void.TYPE))
            source.append("  return rv;\n");

        source.append("}\n\n");
    }

    private static void appendTypeString(StringBuffer buffer, Class type) {
        while (type.isArray()) {
            buffer.append('[');
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            if (type.equals(Boolean.TYPE)) {
                buffer.append('Z');
            } else
                if (type.equals(Long.TYPE)) {
                    buffer.append('J');
                } else {
                    String typeName = type.getName();
                    buffer.append(Character.toUpperCase(typeName.charAt(0)));
                }
        } else {
            buffer.append('L');
            buffer.append(type.getName().replace('.', '/'));
            buffer.append(';');
        }
    }

    private static String getMethodSignature(Method method) {
        Class[] parameterTypes = method.getParameterTypes();
        StringBuffer buffer = new StringBuffer();
        buffer.append('(');
        int count = parameterTypes.length;
        for (int i = 0; i < count; ++i)
            appendTypeString(buffer, parameterTypes[i]);
        buffer.append(')');
        appendTypeString(buffer, method.getReturnType());
        return buffer.toString();
    }

    private static void generateClosureElements(HashMap closureNames, StringBuffer source) {
        int size = closureNames.size();
        Iterator methods = closureNames.keySet().iterator();
        source.append("  static struct { const char* name; const char* sig; } methodElements[").append(size).append("] = {\n");
        while (methods.hasNext()) {
            Method m = (Method) methods.next();
            source.append("    { \"").append(m.getName()).append("\", \"").append(getMethodSignature(m)).append("\" },\n");
        }
        source.append("  };\n");
        source.append("  static jint glueElements[").append(size).append("] = {\n");
        Iterator keys = closureNames.keySet().iterator();
        while (keys.hasNext()) {
            String closureName = (String) closureNames.get(keys.next());
            source.append("    (jint)&").append(closureName).append(",\n");
        }
        source.append("  };\n");
    }

    protected static void generateMethodClosureGlue(Class c, Method[] closures, boolean bundle, StringBuffer source) {
        source.append("static JavaVM* theJVM;\n\n");

        source.append("JNIEXPORT void JNICALL jnidirect_SetJavaVM(JavaVM* jvm)\n");
        source.append("{\n");
        source.append("   theJVM = jvm;\n");
        source.append("}\n\n");

        String classId = mangle(c.getName());
        int count = closures.length;
        HashMap closureNames = new HashMap(count);
        for (int i = 0; i < count; ++i) {
            Method m = closures[i];
            String methodName = m.getName();
            Class returnType = m.getReturnType();
            Class[] parameterTypes = m.getParameterTypes();
            int parameterCount = parameterTypes.length;

            String closureName = "Closure_" + classId + "_" + mangle(methodName);
            closureNames.put(m, closureName);
            source.append("JNIEXPORT ").append(jniTypes.get(returnType)).append(" JNICALL ").append(closureName);
            source.append("(");
            StringBuffer argsBuffer = new StringBuffer("(env, closure->self, closure->method");
            for (int j = 0; j < parameterCount; ++j) {
                if (j > 0)
                    source.append(", ");
                source.append(jniTypes.get(parameterTypes[j]));
                source.append(" arg").append(j);
                argsBuffer.append(", arg").append(j);
            }
            argsBuffer.append(");\n");
            String closureArgs = argsBuffer.toString();
            source.append(")\n");
            source.append("{\n");
            source.append("  JNIEnv* env;\n");
            source.append("  struct { void* glue; jobject self; jmethodID method; } *closure;\n");
            source.append("  asm(\"mr %0,r12\" : \"=r\" (closure));\n");
            source.append("  if ((*theJVM)->GetEnv(theJVM, (void**)&env, JNI_VERSION_1_2) != JNI_OK) {\n");
            if (!returnType.equals(Void.TYPE))
            source.append("    ").append(jniTypes.get(returnType)).append(" rv;\n");
            source.append("    (*theJVM)->AttachCurrentThread(theJVM, (void**)&env, NULL);\n");
            source.append("    ").append(!returnType.equals(Void.TYPE) ? "rv = " : "").append("(*env)->").append(jmethodCallers.get(returnType)).append(closureArgs);
            source.append("    (*theJVM)->DetachCurrentThread(theJVM);\n");
            source.append("  } else {\n");
            source.append("    ").append(!returnType.equals(Void.TYPE) ? "return " : "").append("(*env)->").append(jmethodCallers.get(returnType)).append(closureArgs);
            source.append("  }\n");
            source.append("}\n\n");
        }

        // If loading the library using loadBundle() (as an NSModule), don't generate the JNI_OnLoad function. It will never get called,
        // plus when it looks up the Java class, it may get the wrong one (multiple class loaders).
        if (bundle)
            return;

        // JNI_OnLoad
        source.append("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)\n");
        source.append("{\n");
        source.append("  JNIEnv* env;\n");
        source.append("  jclass linkerClass, objectClass, thisClass;\n");
        source.append("  jmethodID registerMethodClosureFunctions;\n");
        source.append("  jobjectArray methodsArray;\n");
        source.append("  jintArray glueArray;\n");
        generateClosureElements(closureNames, source);
        source.append("  jsize i;\n");
        // source.append("  printf(\"JNI_OnLoad here for class " + c.getName() + "\\n\");\n");
        source.append("  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2) != JNI_OK) return JNI_ERR;\n");
        source.append("  if ((*env)->PushLocalFrame(env, 16) != JNI_OK) return JNI_ERR;\n");
        source.append("  linkerClass = (*env)->FindClass(env, \"jnidirect/Linker\");\n");
        source.append("  if (linkerClass == NULL) goto error;\n");
        source.append("  objectClass = (*env)->FindClass(env, \"java/lang/Object\");\n");
        source.append("  if (objectClass == NULL) goto error;\n");
        source.append("  thisClass = (*env)->FindClass(env, \"").append(c.getName().replace('.', '/')).append("\");\n");
        source.append("  if (thisClass == NULL) goto error;\n");
        source.append("  registerMethodClosureFunctions = (*env)->GetStaticMethodID(env, linkerClass, \"registerMethodClosureFunctions\", \"([Ljava/lang/Object;[I)V\");\n");
        source.append("  if (registerMethodClosureFunctions == NULL) goto error;\n");
        source.append("  methodsArray = (*env)->NewObjectArray(env, ").append(count).append(", objectClass, NULL);\n");
        source.append("  if (methodsArray == NULL) goto error;\n");
        source.append("  for (i = 0; i < ").append(count).append("; ++i) {\n");
        source.append("    jmethodID methodID = (*env)->GetMethodID(env, thisClass, methodElements[i].name, methodElements[i].sig);\n");
        source.append("    jobject methodObject = (*env)->ToReflectedMethod(env, thisClass, methodID, JNI_FALSE);\n");
        source.append("    (*env)->SetObjectArrayElement(env, methodsArray, i, methodObject);\n");
        source.append("    (*env)->DeleteLocalRef(env, methodObject);\n");
        source.append("  }\n");
        source.append("  glueArray = (*env)->NewIntArray(env, ").append(count).append(");\n");
        source.append("  if (glueArray == NULL) goto error;\n");
        source.append("  (*env)->SetIntArrayRegion(env, glueArray, 0, ").append(count).append(", glueElements);\n");
        source.append("  (*env)->CallStaticVoidMethod(env, linkerClass, registerMethodClosureFunctions, methodsArray, glueArray);\n");
        source.append("  (*env)->PopLocalFrame(env, NULL);\n");
        source.append("  theJVM = jvm;\n");
        source.append("  return JNI_VERSION_1_2;\n");
        source.append("error:\n");
        source.append("  (*env)->PopLocalFrame(env, NULL);\n");
        source.append("  return JNI_ERR;\n");
        source.append("}\n\n");
    }

    private static void add(ArrayList list, String[] args) {
        int count = args.length;
        for (int i = 0; i < count; ++i)
            list.add(args[i]);
    }

    /**
     * Generates jnidirect_GetNativeMethods() used to manually registers/unregister
     * the generated native methods associated with a class. This allows native libraries
     * to be dynamically unloadable/replaceable, which is required if classes with native
     * methods are loaded into a class loader.
     *
     * @param c
     * @param nativeMethods
     * @param overloads
     * @param source
     */
    private static void generateBundle(Class c, Method[] nativeMethods, HashMap overloads, StringBuffer source) {
        int count = nativeMethods.length;
        if (count == 0) return;

        // jnidirect_GetNativeMethods
        source.append("JNIEXPORT jint JNICALL jnidirect_GetNativeMethods()\n");
        source.append("{\n");
        source.append("  static JNINativeMethod methods[] = {\n");
        for (int i = 0; i < count; ++i) {
            Method m = nativeMethods[i];
            String name = m.getName();
            source.append("    { \"").append(name).append("\", \"");
            source.append(getMethodSignature(m)).append("\", ");
            source.append("&").append(getMethodIdentifier(c, m, overloads.get(name) == Boolean.TRUE));
            source.append(" },\n");
        }
        source.append("  };\n");
        source.append("  return (jint) &methods;\n");
        source.append("}\n\n");
    }

    private static void loadBundle(Class c, Method[] nativeMethods, File glueLibrary) {
        try {
            NSModule module = new NSModule(glueLibrary);
            module.registerNativeMethods(c, nativeMethods);
        } catch (IOException ioEx) {
        }
    }

    protected static File generateGlueLibrary(Class c, String source, String[] frameworks) {
        try {
            String classPrefix = "jnidirect_" + mangle(c.getName()) + "_";
            File sourceFile = File.createTempFile(classPrefix, ".c");
            if (!kKeepTemporaryFiles) sourceFile.deleteOnExit();
            BufferedWriter output = new BufferedWriter(new FileWriter(sourceFile));
            output.write(source);
            output.close();

            File libraryFile = File.createTempFile(classPrefix, ".jnilib");
            if (!kKeepTemporaryFiles) libraryFile.deleteOnExit();

            String defaultArgs[] = {
                "cc", "-Os", "-g", "-bundle", "-I/System/Library/Frameworks/JavaVM.framework/Headers",
                sourceFile.getPath(), "-o", libraryFile.getPath()
            };
            ArrayList list = new ArrayList();
            add(list, defaultArgs);
            if (frameworks != null) {
                for (int i = frameworks.length - 1; i >= 0; --i) {
                    list.add("-framework");
                    list.add(frameworks[i]);
                }
            }
            String[] args = new String[list.size()];
            list.toArray(args);
            Process compiler = Runtime.getRuntime().exec(args);
            int status = compiler.waitFor();
            if (status == 0)
                return libraryFile;
            BufferedReader reader = new BufferedReader(new InputStreamReader(compiler.getErrorStream()));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            libraryFile.delete();
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * This demonstrates the concept for the new JNIDirect implementation. It generates a glue source file
     * and jnilib on the fly, loads it, and calls the native method. Conceptually, this is the same thing
     * JDirect does, but it uses the host C compiler to handle all the complications of dealing with
     * object code.
     */
    public static void test() {
        /*
        try {
            File sourceFile = File.createTempFile("JNIDirectLinkerTest", ".c");
            sourceFile.deleteOnExit();
            BufferedWriter source = new BufferedWriter(new FileWriter(sourceFile));

            source.write("#include <stdio.h>\n");
            source.write("#include <jni.h>\n");
            source.write("JNIEXPORT jint JNICALL Java_Test_printf(JNIEnv *env, jclass thisClass, jbyteArray array)\n");
            source.write("{\n");
            source.write("	jboolean isCopy;\n");
            source.write("	jbyte* arrayBytes = (*env)->GetByteArrayElements(env, array, &isCopy);\n");
            source.write("	jint result = printf(arrayBytes);\n");
            source.write("	(*env)->ReleaseByteArrayElements(env, array, arrayBytes, JNI_COMMIT);\n");
            source.write("}\n");
            source.close();

            File libraryFile = File.createTempFile("JNIDirectLinkerTest", ".jnilib");
            libraryFile.deleteOnExit();
            String args[] = {
                "cc", "-bundle", "-I/System/Library/Frameworks/JavaVM.framework/Headers",
                sourceFile.getPath(), "-o", libraryFile.getPath()
            };
            Process compiler = Runtime.getRuntime().exec(args);
            int status = compiler.waitFor();

            System.load(libraryFile.getPath());
            Test.write("dynamically generated call to printf here!\n".getBytes());
        } catch (IOException ioex) {
        } catch (InterruptedException intex) {
        }
        */
    }

    private static void registerMethodClosureFunctions(Object[] methods, int[] functions) {
        int size = methods.length;
        for  (int i = 0; i < size; ++i)
            MethodClosure.functions.put(methods[i], new Integer(functions[i]));
    }

    public static int newMethodClosure(Method method, Object self) {
        return MethodClosure.create(method, self);
    }

    public static void disposeMethodClosure(int closure) {
        MethodClosure.dispose(closure);
    }
}

abstract class MethodClosure {
    static HashMap functions = new HashMap();

    private static Integer getMethodClosureFunction(Method method) {
        Integer function = (Integer) functions.get(method);
        if (function == null) {
            Class c = method.getDeclaringClass();
            NSModule module = NSModule.getRegisteredModule(c);
            if (module != null) {
                // perhaps we can look it up by name...
                // XXX move the code for generating closureName out of here...
                String classId = Linker.mangle(c.getName());
                String methodId = Linker.mangle(method.getName());
                String closureName = "Closure_" + classId + "_" + methodId;
                int f = module.getSymbolAddress(closureName);
                if (f != 0) {
                    return new Integer(f);
                }
            }
            // hasn't been generated yet, generate it here? talk about late binding!
            // this is much less efficient than providing the closures up front, but it
            // can be used with classes that don't otherwise have native methods.
            StringBuffer source = new StringBuffer();
            source.append("#include <jni.h>\n\n");
            Linker.generateMethodClosureGlue(c, new Method[] { method }, false, source);
            File glueLibrary = Linker.generateGlueLibrary(c, source.toString(), null);
            if (glueLibrary != null) {
                System.load(glueLibrary.getPath());
                function = (Integer) functions.get(method);
            }
        }
        return function;
    }

    /**
     * Generates a unique function pointer that encapsulates a call to a the specified
     * method with the specified object. This relies on the generation of C glue functions
     * that are associated with this class from a call to registerMethodClosureFunctions above.
     */
    protected static int create(Method method, Object self) {
        Integer function = getMethodClosureFunction(method);
        int[] stub = {
            0x3D800000, 0x618C0000, 0x800C0000, 0x7C0903A6, 0x4E800420,
            function.intValue(), $jni$NewWeakGlobalRef$(self), $jni$FromReflectedMethod$(method)
        };
        int stubSize = stub.length * 4;
        int stubPtr = malloc(stubSize);
        int closurePtr = stubPtr + 20;
        stub[0] |= (closurePtr >>> 16);
        stub[1] |= (closurePtr & 0xFFFF);
        memcpy(stubPtr, stub, stubSize);
        // Is this necessary on Mac OS X?
        // MakeDataExecutable(stubPtr, glueSize);
        return stubPtr;
    }

    protected static void dispose(int closure) {
        // FIXME should delete the weak global ref to the object contained within the closure vector.
        int selfRef = $read$int$(closure + 24);
        if (selfRef != 0) $jni$DeleteWeakGlobalRef$(selfRef);
        free(closure);
    }

    private static native int $jni$NewWeakGlobalRef$(Object object);
    private static native void $jni$DeleteWeakGlobalRef$(int weakref);
    private static native int $jni$FromReflectedMethod$(Method method);
    private static native int $read$int$(int pointer);

    private static native int malloc(int size);
    private static native void free(int pointer);
    private static native int memcpy(int destination, int[] source, int size);
    // private static native void MakeDataExecutable(int baseAddress, int length);

    static {
        // String[] frameworks = { "Carbon" };
        // link(MethodClosure.class, frameworks);
        Linker.link(MethodClosure.class);
    }
}
