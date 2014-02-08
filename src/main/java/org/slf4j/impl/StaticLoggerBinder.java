package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;
import com.github.aselab.sbt.slf4j.LoggerFactory;

public class StaticLoggerBinder implements LoggerFactoryBinder {
  private final ILoggerFactory loggerFactory;

  private static final StaticLoggerBinder instance = new StaticLoggerBinder();

  private StaticLoggerBinder() {
    loggerFactory = new LoggerFactory();
  }

  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  public String getLoggerFactoryClassStr() {
    return LoggerFactory.class.getName();
  }

  public static StaticLoggerBinder getSingleton() {
    return instance;
  }
}
