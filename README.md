# ClockBlocked

Creates a "wandering portal" that cycles from frame to frame on every server restart (or every x minutes on a timer).

## Usage

1. Create portals as usual
2. Give each portal a WorldGuard region, with the same name being used in the overworld and nether
3. Add region names to config.yml, in the order you want them to cycle in.

Every time the server shuts down, it will advance the currently selected portal. (This way it won't advance after a crash, and won't be off-by-one when you start it the first time.) When the server comes up, the plugin will ensure that all of the portals are not lit, and light the currently selected one. A command may also be used to cycle the portal rotation manually.


## Portal Considerations

* Portal WorldGuard regions *must be cuboids*
* As the plugin replaced air blocks with portal blocks, *the WorldGuard region must not encompass air outside of the portal frame.* Examples: Rectangular regions encompassing the frame work. Portals with a rectangular center and missing corners work if the region only encloses the center and not the frame. A circular portal would work if it had something built around it, so the region corners overlap non-air materials.
* The most effective way to prepare portals is to build the frame, then use WorldEdit to add the portal blocks, using //fast (which causes block and lighting updates to be delayed until the edit is done) and then //set or //replace to add portal:1 or portal:2 (depending on the portal orientation).

![Portal Regions](http://i.imgur.com/pC0mf0z.png)


## Commands

The `clockblocked.admin` command allows you to access `/clockblocked` (shorthand: `/clbl`), which has several subcommands that allow you to manipulate the portals. Running the command will list those available, which include:

* current - Check current active portal
* cycle - Advance active portal by one
* list - List registered portals
* light <portal> - Make a specific portal active
* lightall - Open all registered portals temporarily. (Cycling resumes normally on restart.)
* unlightall - Close all registerd portals temporarily. (Cycling resumes normally on restart.)
* reload - Reload the plugin configuration