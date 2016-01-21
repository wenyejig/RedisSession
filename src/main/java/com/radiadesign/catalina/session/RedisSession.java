/*    */ package com.radiadesign.catalina.session;
/*    */ 
/*    */ import java.security.Principal;
/*    */ import java.util.HashMap;
/*    */ import java.util.logging.Logger;
/*    */ import org.apache.catalina.Manager;
/*    */ import org.apache.catalina.session.StandardSession;
/*    */ 
/*    */ public class RedisSession extends StandardSession
/*    */ {
/* 13 */   private static Logger log = Logger.getLogger("RedisSession");
/*    */ 
/* 15 */   protected static Boolean manualDirtyTrackingSupportEnabled = Boolean.valueOf(false);
/*    */ 
/* 21 */   protected static String manualDirtyTrackingAttributeKey = "__changed__";
/*    */   protected HashMap<String, Object> changedAttributes;
/*    */   protected Boolean dirty;
/*    */ 
/*    */   public static void setManualDirtyTrackingSupportEnabled(Boolean enabled)
/*    */   {
/* 18 */     manualDirtyTrackingSupportEnabled = enabled;
/*    */   }
/*    */ 
/*    */   public static void setManualDirtyTrackingAttributeKey(String key)
/*    */   {
/* 24 */     manualDirtyTrackingAttributeKey = key;
/*    */   }
/*    */ 
/*    */   public RedisSession(Manager manager)
/*    */   {
/* 32 */     super(manager);
/* 33 */     resetDirtyTracking();
/*    */   }
/*    */ 
/*    */   public Boolean isDirty() {
/* 37 */     return Boolean.valueOf((this.dirty.booleanValue()) || (!this.changedAttributes.isEmpty()));
/*    */   }
/*    */ 
/*    */   public HashMap<String, Object> getChangedAttributes() {
/* 41 */     return this.changedAttributes;
/*    */   }
/*    */ 
/*    */   public void resetDirtyTracking() {
/* 45 */     this.changedAttributes = new HashMap();
/* 46 */     this.dirty = Boolean.valueOf(false);
/*    */   }
/*    */ 
/*    */   public void setAttribute(String key, Object value) {
/* 50 */     if ((manualDirtyTrackingSupportEnabled.booleanValue()) && (manualDirtyTrackingAttributeKey.equals(key))) {
/* 51 */       this.dirty = Boolean.valueOf(true);
/* 52 */       return;
/*    */     }
/*    */ 
/* 55 */     Object oldValue = getAttribute(key);
/* 56 */     if (((value == null) && (oldValue != null)) || ((oldValue == null) && (value != null)) || 
/* 58 */       (!value
/* 58 */       .getClass().isInstance(oldValue)) || 
/* 59 */       (!value
/* 59 */       .equals(oldValue)))
/*    */     {
/* 60 */       this.changedAttributes.put(key, value);
/*    */     }
/*    */ 
/* 63 */     super.setAttribute(key, value);
/*    */   }
/*    */ 
/*    */   public void removeAttribute(String name) {
/* 67 */     this.dirty = Boolean.valueOf(true);
/* 68 */     super.removeAttribute(name);
/*    */   }
/*    */ 
/*    */   public void setId(String id)
/*    */   {
/* 76 */     this.id = id;
/*    */   }
/*    */ 
/*    */   public void setPrincipal(Principal principal) {
/* 80 */     this.dirty = Boolean.valueOf(true);
/* 81 */     super.setPrincipal(principal);
/*    */   }
/*    */ }

/* Location:           /Users/wench/code/apache-tomcat-session/lib/tomcat-redis-session-manager-1.2-tomcat-7-java-7-1.0.jar
 * Qualified Name:     com.radiadesign.catalina.session.RedisSession
 * JD-Core Version:    0.6.2
 */