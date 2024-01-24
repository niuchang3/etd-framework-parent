package org.etd.framework.starter.log.config;

import org.slf4j.AutuLogMDCAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Young
 * @description
 * @date 2020/12/16
 */

public class MDCAdapterInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        AutuLogMDCAdapter.getInstance();
    }


}
