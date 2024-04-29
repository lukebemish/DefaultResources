# DefaultResources

[![CodeFactor](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources/badge?style=for-the-badge)](https://www.codefactor.io/repository/github/lukebemishprojects/defaultresources)
[![Version](https://img.shields.io/maven-central/v/dev.lukebemish/defaultresources?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v)](https://central.sonatype.com/artifact/dev.lukebemish/defaultresources/)

A tool for providing default "resources" (assets, world-specific data, or other data) for a mod. Data is bundled in the `defaultresources` folder of the mod jar, and is read from that folder at runtime. Data can be extracted from that folder into `globalresources` by defaultresources when a user wants to modify it, through the defaultresources config. The behavior of `defaulrresources` for a single mod is controlled by the `defaultresources.meta.json` file.

The core of DefaultResources is `GlobalResourceManager`, which provides a vanilla `ResourceManager` for "global" assets and data.
This data is loaded from the `gobaldata` or `globalassets` folders of mods, detected global resource packs, or archives or
folders in the `globalresources` folder. Mods can also bundle "default" resources, in a `defaultresources` folder by default,
that will be loaded or extracted depending on mod configuration.

For an example use case, see [Excavated Variants](https://github.com/lukebemish/excavated_variants)

## Depending on DefaultResources

Available versions of DefaultResources are tagged on this repository. DefaultResources is, as of Minecraft 1.20.5, available on maven central at `dev.lukebemish:defaultresources`; versions for different loaders may be selected by capability.

Adding artifact on NeoForge:
```gradle
dependencies {
    implementation('dev.lukebemish:defaultresources:<version>') {
        capabilities {
            requireCapability('dev.lukebemish:defaultresources-neoforge')
        }
    }
}
```

Adding artifact on Quilt or Fabric
```gradle
dependencies {
    modImplementation('dev.lukebemish:defaultresources:<version>') {
        capabilities {
            requireCapability('dev.lukebemish:defaultresources-fabric')
        }
    }
}
```

For versions before Minecraft 1.20.5, DefaultResources is available at `https://maven.lukebemish.dev/releases/` under loader and Minecraft version specific coordinates:

Adding artifact on NeoForge
```gradle
dependencies {
    implementation 'dev.lukebemish.defaultresources:defaultresources-neoforge-<minecraft version>:<version>'
}
```

Adding artifact on Quilt or Fabric
```gradle
dependencies {
    modImplementation 'dev.lukebemish.dynamicassetgenerator:defaultresources-fabriquilt-<minecraft version>:<version>'
}
```

## Meta File Format

The `defaultresources.meta.json` file defines how DefaultResources should treat your mod. It takes the following fields:

* `resources_path` - the path to default resources within the mod jar. Optional; defaults to `defaultresources`
* `zip` - whether, when extracted, the resources should be zipped. Optional; defaults to true.
* `extract` - whether the bundled default resources should be extracted and kept up to date by default. Optional; defaults to true. A hash is saved alongside the resources so that user modifications will not be overwritten.
* `data_version` - a version number for the bundled default resources. Optional. Saved alongside the resources so that mods can be given more context, through a registered `OutdatedResourcesListener`, if their resources to extract could not be extracted due to user modifications.

## Detection from `resourcepacks`

DefaultResources will automatically detect resource packs that include "global" resources and attempt to configure them.
To mark that your resource pack should be detected, add the following to the `pack.mcmeta`:
```json
{
  "defaultresources": {
    "detect": true
  }
}
```
Which such resource packs are loaded as global resources can be configured in the DefaultResources config.
