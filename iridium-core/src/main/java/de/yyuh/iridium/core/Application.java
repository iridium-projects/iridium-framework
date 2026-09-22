package de.yyuh.iridium.core;

import de.yyuh.iridium.core.bean.BeanPool;

public abstract class Application {

    public Application() {
        BeanPool.initialize();
    }
}