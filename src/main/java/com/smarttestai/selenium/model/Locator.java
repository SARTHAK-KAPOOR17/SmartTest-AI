package com.smarttestai.selenium.model;

import java.util.Objects;

public class Locator {
    private LocatorStrategy strategy;
    private String value;

    public Locator() {
    }

    public Locator(LocatorStrategy strategy, String value) {
        this.strategy = strategy;
        this.value = value;
    }

    public LocatorStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(LocatorStrategy strategy) {
        this.strategy = strategy;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locator locator = (Locator) o;
        return strategy == locator.strategy && Objects.equals(value, locator.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, value);
    }

    @Override
    public String toString() {
        return "Locator{" +
                "strategy=" + strategy +
                ", value='" + value + '\'' +
                '}';
    }
}
