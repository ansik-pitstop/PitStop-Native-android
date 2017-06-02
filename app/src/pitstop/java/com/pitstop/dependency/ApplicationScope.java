package com.pitstop.dependency;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Created by Karol Zdebel on 6/2/2017.
 */

@Scope
@Retention(RetentionPolicy.CLASS)
public @interface ApplicationScope {
}
