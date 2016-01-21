/*    */ package com.radiadesign.catalina.session;
/*    */ 
/*    */ import java.io.BufferedInputStream;
/*    */ import java.io.BufferedOutputStream;
/*    */ import java.io.ByteArrayInputStream;
/*    */ import java.io.ByteArrayOutputStream;
/*    */ import java.io.IOException;
/*    */ import java.io.ObjectInputStream;
/*    */ import java.io.ObjectOutputStream;
/*    */ import javax.servlet.http.HttpSession;
/*    */ import org.apache.catalina.util.CustomObjectInputStream;
/*    */ 
/*    */ public class JavaSerializer
/*    */   implements Serializer
/*    */ {
/*    */   private ClassLoader loader;
/*    */ 
/*    */   public void setClassLoader(ClassLoader loader)
/*    */   {
/* 14 */     this.loader = loader;
/*    */   }
/*    */ 
/*    */   public byte[] serializeFrom(HttpSession session)
/*    */     throws IOException
/*    */   {
/* 20 */     RedisSession redisSession = (RedisSession)session;
/* 21 */     ByteArrayOutputStream bos = new ByteArrayOutputStream();
/* 22 */     ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos)); Throwable localThrowable3 = null;
/*    */     try { oos.writeLong(redisSession.getCreationTime());
/* 24 */       redisSession.writeObjectData(oos);
/*    */     }
/*    */     catch (Throwable localThrowable1)
/*    */     {
/* 22 */       localThrowable3 = localThrowable1; throw localThrowable1;
/*    */     }
/*    */     finally {
/* 25 */       if (oos != null) if (localThrowable3 != null) try { oos.close(); } catch (Throwable localThrowable2) { localThrowable3.addSuppressed(localThrowable2); } else oos.close();
/*    */     }
/* 27 */     return bos.toByteArray();
/*    */   }
/*    */ 
/*    */   public HttpSession deserializeInto(byte[] data, HttpSession session)
/*    */     throws IOException, ClassNotFoundException
/*    */   {
/* 33 */     RedisSession redisSession = (RedisSession)session;
/*    */ 
/* 35 */     BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
/*    */ 
/* 37 */     ObjectInputStream ois = new CustomObjectInputStream(bis, this.loader);
/* 38 */     redisSession.setCreationTime(ois.readLong());
/* 39 */     redisSession.readObjectData(ois);
/*    */ 
/* 41 */     return session;
/*    */   }
/*    */ }

/* Location:           /Users/wench/code/apache-tomcat-session/lib/tomcat-redis-session-manager-1.2-tomcat-7-java-7-1.0.jar
 * Qualified Name:     com.radiadesign.catalina.session.JavaSerializer
 * JD-Core Version:    0.6.2
 */