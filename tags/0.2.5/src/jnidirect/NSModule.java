/*
 * JNIDirect provides runtime JNI native method generation.
 *
 * Copyright (C) 2002-2003 by Patrick C. Beard <pcbeard@mac.com>
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
//  NSModule.java
//

package jnidirect;

import java.io.*;
import java.util.WeakHashMap;
import java.lang.reflect.Method;

/**
 * Encapsulates a mach-o library built with the -bundle option, which allows it to
 * be dynamically loaded and unloaded. This is used by jnidirect.Linker.link() to
 * work around Java's builtin System.load(), which loads a .jnilib once, into a global
 * namespace. This won't work with classes with native methods loaded into a class
 * loader.
 */
public class NSModule {
    private int image;
    private int module;

    /*
     typedef enum {
         NSObjectFileImageFailure = 0,	// for this a message is printed on stderr
         NSObjectFileImageSuccess,
         NSObjectFileImageInappropriateFile,
         NSObjectFileImageArch,
         NSObjectFileImageFormat,		// for this a message is printed on stderr
         NSObjectFileImageAccess
     } NSObjectFileImageReturnCode;
     */

    private static final int NSObjectFileImageFailure = 0;
    private static final int NSObjectFileImageSuccess = 1;

    private static final int NSLINKMODULE_OPTION_PRIVATE = 0x2;

    public NSModule(File f) throws IOException {
        byte[] path = (f.getPath() + '\0').getBytes();
        int[] outImage = { 0 };
        int rv = NSCreateObjectFileImageFromFile(path, outImage);
        if (rv != NSObjectFileImageSuccess)
            throw new IOException("NSCreateObjectFileImageFromFile returned " + rv);
        image = outImage[0];
        module = NSLinkModule(image, (f.getName() + '\0').getBytes(), NSLINKMODULE_OPTION_PRIVATE);
        if (module == 0)
            throw new IOException("NSLinkModule returned null");
    }

    int getSymbolAddress(String symbolName) {
        int symbol = NSLookupSymbolInModule(module, ("_" + symbolName + '\0').getBytes());
        if (symbol != 0)
            return NSAddressOfSymbol(symbol);
        return 0;
    }

    /**
     * Support jnidirect.Linker.loadBundle() by managing the lifetime
     * of NSModules that contain JNI native methods by associating
     * the class with the NSModule using a WeakHashMap. When the class is
     * garbage collected, the NSModule will be finalized, which will unload
     * the native library.
     */
    private static WeakHashMap registeredModules = new WeakHashMap();
    private boolean registeredModule = false;

    boolean registerNativeMethods(Class c, Method[] nativeMethods) {
        int f = getSymbolAddress("jnidirect_SetJavaVM");
        if (f != 0) {
            $call$(f, $jni$GetJavaVM$());
        }
        f = getSymbolAddress("jnidirect_GetNativeMethods");
        if (f != 0) {
            int methods = $call$(f);
            int nMethods = nativeMethods.length;
            if ($jni$RegisterNatives$(c, methods, nMethods) == 0) {
                registeredModules.put(c, this);
                registeredModule = true;
                return true;
            }
        }
        return false;
    }

    static NSModule getRegisteredModule(Class c) {
        return (NSModule) registeredModules.get(c);
    }

    protected boolean unload() {
        return NSUnLinkModule(module, 0) && NSDestroyObjectFileImage(image);
    }

    protected void finalize() {
        if (registeredModule)
            unload();
    }
    
    public static void test() {
        try {
            NSModule module = new NSModule(new File("BundleOfJoy.bundle/Contents/MacOS/BundleOfJoy"));
            int getOdeToJoy = module.getSymbolAddress("getOdeToJoy");
            if (getOdeToJoy != 0) {
                printf("getOdeToJoy() = %s\n\0".getBytes(), $call$(getOdeToJoy));
                System.out.println("NSModule test successful.");
            } else {
                System.out.println("NSModule test failed.");
            }
            if (module.unload())
                System.out.println("NSModule unloaded.");
            else
                System.out.println("NSModule failed to unload.");
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /*
     http://www.osxfaq.com/man/3/NSModule.ws
     // limited implementation, only MH_BUNDLE files can be used
     extern NSObjectFileImageReturnCode NSCreateObjectFileImageFromFile(const char *pathName,
                                                                        NSObjectFileImage *objectFileImage);
     extern NSObjectFileImageReturnCode NSCreateObjectFileImageFromMemory(void *address,
                                                                          unsigned long size,
                                                                          NSObjectFileImage *objectFileImage);
     extern enum DYLD_BOOL NSDestroyObjectFileImage(NSObjectFileImage objectFileImage);
     
     // limited implementation, only MH_BUNDLE files can be linked
     extern NSModule NSLinkModule(NSObjectFileImage objectFileImage,
                                  const char *moduleName,
                                  unsigned long options);
     #define NSLINKMODULE_OPTION_NONE		0x0
     #define NSLINKMODULE_OPTION_BINDNOW		0x1
     #define NSLINKMODULE_OPTION_PRIVATE		0x2
     #define NSLINKMODULE_OPTION_RETURN_ON_ERROR	0x4

     // limited implementation, only modules loaded with NSLinkModule() can be unlinked
     extern enum DYLD_BOOL NSUnLinkModule(NSModule module,
                                           unsigned long options);
     #define NSUNLINKMODULE_OPTION_NONE			0x0
     #define NSUNLINKMODULE_OPTION_KEEP_MEMORY_MAPPED	0x1
     #define NSUNLINKMODULE_OPTION_RESET_LAZY_REFERENCES	0x2

     extern NSSymbol NSLookupSymbolInModule(NSModule module,
                                            const char *symbolName);

     extern void * NSAddressOfSymbol(NSSymbol symbol);
    */

    private static native int NSCreateObjectFileImageFromFile(byte[] pathName, int[] objectFileImage);
    private static native int NSCreateObjectFileImageFromMemory(int address, int size, int[] objectFileImage);
    private static native boolean NSDestroyObjectFileImage(int objectFileImage);
    private static native int NSLinkModule(int objectFileImage, byte[] moduleName, int options);
    private static native boolean NSUnLinkModule(int module, int options);
    private static native int NSLookupSymbolInModule(int module, byte[] symbolName);
    private static native int NSAddressOfSymbol(int symbol);

    private static native int $call$(int f);
    private static native int $call$(int f, int arg);
    private static native int $jni$GetJavaVM$();
    private static native int $jni$RegisterNatives$(Class clazz, int methods, int nMethods);
    private static native int printf(byte[] format, int string);

    static {
        jnidirect.Linker.link(NSModule.class);
    }
}
