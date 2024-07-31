package noname.sidestep.configStore

import android.content.Context
import org.amnezia.awg.config.Config

class SingularConfigStore : ConfigStore {
    val conf: Config

    constructor(ctx: Context) {
        conf = Config.parse(ctx.assets.open("main.conf"));
    }

    override fun create(name: String, config: Config): Config {
        TODO("Not implemented")
    }

    override fun delete(name: String) {
        TODO("Not implemented")
    }

    override fun enumerate(): Set<String> {
        return setOf("Sidestep");
    }

    override fun load(name: String): Config {
        return conf;
    }

    override fun rename(name: String, replacement: String) {
        TODO("Not implemented")
    }

    override fun save(name: String, config: Config): Config {
        TODO("Not implemented")
    }
}