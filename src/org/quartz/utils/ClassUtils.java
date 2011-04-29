package org.quartz.utils;

import java.lang.annotation.Annotation;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.StatefulJob;

public class ClassUtils {

    
    public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> a) {

        if (clazz == null) 
            return false;

        if(clazz.isAnnotationPresent(a))
            return true;
        
        if(isAnnotationPresentOnSuperClasses(clazz, a))
            return true;

        if(isAnnotationPresentOnInterfaces(clazz, a))
            return true;
        
        return false;
    }
    
    private static boolean isAnnotationPresentOnSuperClasses(Class<?> clazz, Class<? extends Annotation> a) {
        
        if (clazz == null) 
            return false;

        Class<?> c = clazz.getSuperclass();
        while(c != null && !c.equals(Object.class)) {
            if( c.isAnnotationPresent(a) )
                return true;
            if(isAnnotationPresentOnInterfaces(c, a))
                return true;
            c = c.getSuperclass();
        }
        
        if(isAnnotationPresentOnInterfaces(clazz.getSuperclass(), a))
            return true;
        
        return false;
    }

    private static boolean isAnnotationPresentOnInterfaces(Class<?> clazz, Class<? extends Annotation> a) {
        
        if (clazz == null) 
            return false;

        for(Class<?> i : clazz.getInterfaces()) {
            if( i.isAnnotationPresent(a) )
                return true;
            if(isAnnotationPresentOnInterfaces(i, a))
                return true;
        }
        
        return false;
    }
}
