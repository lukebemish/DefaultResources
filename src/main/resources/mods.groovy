MultiplatformModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[1,)'
    issueTrackerUrl = 'https://github.com/lukebemishprojects/DefaultResources/issues'
    license = 'LGPL-3.0-or-later'

    mod {
        modId = buildProperties.mod_id
        displayName = buildProperties.mod_name
        version = environmentInfo.version
        displayUrl = 'https://github.com/lukebemishprojects/DefaultResources'
        contact {
            sources = 'https://github.com/lukebemishprojects/DefaultResources'
        }
        author = 'Luke Bemish'
        description = buildProperties.description

        entrypoints {
            main = 'dev.lukebemish.defaultresources.impl.fabriquilt.DefaultResourcesFabriQuilt'
            client = 'dev.lukebemish.defaultresources.impl.fabriquilt.DefaultResourcesFabriQuiltClient'
        }

        dependencies {
            mod 'minecraft', {
                def minor = libs.versions.minecraft.split(/\./)[1] as int
                versionRange = "[${libs.versions.minecraft},1.${minor+1}.0)"
            }
            onNeoForge {
                neoforge = ">=${libs.versions.neoforge}"
            }
            onFabric {
                mod 'fabricloader', {
                    versionRange = ">=${libs.versions.fabric_loader}"
                }
                mod 'fabric-api', {
                    versionRange = ">=${libs.versions.fabric_api.split(/\+/)[0]}"
                }
            }
        }
    }

    onFabric {
        mixins {
            mixin 'mixin.defaultresources.fabriquilt.json'
            mixin 'mixin.defaultresources.json'
        }
    }
    onNeoForge {
        mixins {
            mixin 'mixin.defaultresources.json'
        }
    }
}
