package com.etd.framework.provider;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;

import java.util.Arrays;
import java.util.List;

public class EtdProviderManager extends ProviderManager {


    public EtdProviderManager(AuthenticationProvider... providers) {
        super(Arrays.asList(providers), null);
    }

    /**
     * Construct a {@link ProviderManager} using the given {@link AuthenticationProvider}s
     *
     * @param providers the {@link AuthenticationProvider}s to use
     */
    public EtdProviderManager(List<AuthenticationProvider> providers) {
        super(providers, null);
    }

    /**
     * Construct a {@link ProviderManager} using the provided parameters
     *
     * @param providers the {@link AuthenticationProvider}s to use
     * @param parent    a parent {@link AuthenticationManager} to fall back to
     */
    public EtdProviderManager(List<AuthenticationProvider> providers, AuthenticationManager parent) {
        super(providers, parent);
    }
}
