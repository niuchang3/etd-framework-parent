package org.etd.framework.starter.state.builder;


public interface From<S> {

    To<S> from(S states);
}
