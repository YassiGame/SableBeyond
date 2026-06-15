# Changelog

<!--
All notable changes to this project should be documented in this file.

When you bump `mod_version` in `gradle.properties`, add a matching section here before merging to `main`.

Example:

## [0.1.1] - The Aeronautics Update

- Added a new feature.
- Fixed a crash on startup.
-->

## [0.0.1] - The starting point

- (Neoforge) Added mechanical arm compatibility with Sable sublevels.
- (Neoforge) Added the ability for Create fans to push sublevels.
- (Common) Added mass for entities, configurable with formulas, etc.
- That’s all, I think. Apart from a few things like experimental player interaction / mass, nothing major, and in any case they’re still very much WIP.

## [0.0.2] - Dynamic Mass and Config Revamp and ~~cake~~ KubeJS !

<h1>
⚠️ This update breaks the current configuration. The config path has changed, so please back up your old config and delete it to allow the new one to be created.
</h1>

- (Common) Added Cloth Config for a better configuration screen
- (Common) Added the Dynamic Mass API
- (NeoForge) Added Dynamic Mass implementation for Create fluid tanks, spouts, drains and basins
- (NeoForge) Added KubeJS support (documentation will be available soon, and more features will be added later)
- (Common) Added a command to display system information for GitHub issue reports
- (NeoForge) Create Basins inside upside-down sub-levels can now drop their items (disabled by default)
- (Common) Added French translation
- (Common) Added a new config entry for living entity mass. It is now possible to apply mass only to players
- (Common) Added a Sable Beyond button to the title screen and pause screen for easy access to the config (can be disabled)
- (Common) Dynamic Mass and Entity Mass are now disabled by default (change this in the config)

## [0.0.3] - Dynamic Mass hotfix
- (Common) Dynamic mass now correctly syncs changes when a block with dynamic mass is broken.

## [0.4.0] - Fire Update ? (Like, not “fire” as in a cool update but I mean literal fire update. Ahhh please send help...)
- (Fabric) Sorry Fabric users iam lwk silly and didn't notice the dynamic mass crash at startup, now its fixed
- (Common) Adding a better fire spreading on sublevels plus fire goes out when the sublevel is in water or in a solid block
- (NeoForge) Added some fixes of mechanical hand (stolen) from sable (just temporary, waiting for the official fix)
- (NeoForge) Temporary support for the sprinkler of the create addon slice and dice, now working with sable sublevels
- (NeoForge) Added support to the sable addon player ragdoll, now the entity mass is not applied on ragdolls sublevel

## [0.4.1] - Hot Fix mixins and server crash
- (Common) Updated fire mixin code to improve compatibility with other mods (like ftbchunk)
- (NeoForge) Dynamic mass now initializes once the server is ready instead of during startup, preventing a potential crash

## [0.4.2] - Hot Fix Sable v2.0.1 entity mass crash
- (Common) Some FireMixin clean up
- (NeoForge) Fixed a crash of EntityMass with Sable v2.0.1
