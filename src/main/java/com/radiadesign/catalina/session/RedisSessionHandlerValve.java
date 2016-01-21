/*    */ package com.radiadesign.catalina.session;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import javax.servlet.ServletException;
/*    */ import org.apache.catalina.Session;
/*    */ import org.apache.catalina.Valve;
/*    */ import org.apache.catalina.connector.Request;
/*    */ import org.apache.catalina.connector.Response;
/*    */ import org.apache.catalina.valves.ValveBase;
/*    */ import org.apache.juli.logging.Log;
/*    */ import org.apache.juli.logging.LogFactory;
/*    */ 
/*    */ public class RedisSessionHandlerValve extends ValveBase
/*    */ {
/* 15 */   private final Log log = LogFactory.getLog(RedisSessionManager.class);
/*    */   private RedisSessionManager manager;
/*    */ 
/*    */   public void setRedisSessionManager(RedisSessionManager manager)
/*    */   {
/* 19 */     this.manager = manager;
/*    */   }
/*    */ 
/*    */   public void invoke(Request request, Response response) throws IOException, ServletException
/*    */   {
/*    */     try {
/* 25 */       getNext().invoke(request, response);
/*    */     }
/*    */     finally
/*    */     {
/*    */       Session session= request.getSessionInternal(false);
/* 28 */       storeOrRemoveSession(session);
/* 29 */       this.manager.afterRequest();
/*    */     }
/*    */   }
/*    */ 
/*    */   private void storeOrRemoveSession(Session session) {
/*    */     try {
/* 35 */       if (session != null)
/* 36 */         if (session.isValid()) {
/* 37 */           this.log.trace("Request with session completed, saving session " + session.getId());
/* 38 */           if (session.getSession() != null) {
/* 39 */             this.log.trace("HTTP Session present, saving " + session.getId());
/* 40 */             this.manager.save(session);
/*    */           }
/*    */           else {
/* 43 */             this.log.trace("No HTTP Session present, Not saving " + session.getId());
/*    */           }
/*    */         } else {
/* 46 */           this.log.trace("HTTP Session has been invalidated, removing :" + session.getId());
/* 47 */           this.manager.remove(session);
/*    */         }
/*    */     }
/*    */     catch (Exception localException)
/*    */     {
/*    */     }
/*    */   }
/*    */ }

/* Location:           /Users/wench/code/apache-tomcat-session/lib/tomcat-redis-session-manager-1.2-tomcat-7-java-7-1.0.jar
 * Qualified Name:     com.radiadesign.catalina.session.RedisSessionHandlerValve
 * JD-Core Version:    0.6.2
 */