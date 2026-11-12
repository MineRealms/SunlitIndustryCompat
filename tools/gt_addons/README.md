# GT addon recipe validation

Use an isolated Forge 1.20.1 server with the versions listed in
`docs/GT_ADDON_LINKS_20261112.md`. GTCA must include the two source recipe fixes.
Include the current pack's linkage, heavy-alloy, rocket, fuel, casing and phase-4
repair scripts. Enable the pack's GTNN hard-line settings.

Copy `gtcaValidation.js.snippet` into the fixture's `kubejs/server_scripts` with
the `.js` extension. This is a test probe, not a production pack payload.
On startup it exports `gtca-recipe-validation.json` under the fixture root.
After a server `/reload`, run `gtca_validate` in the console to export a fresh
snapshot. The command requires permission level 2.

```powershell
node tools/gt_addons/verify_snapshot.cjs G:/emoney-gtca-link-test-20261112/gtca-recipe-validation.json
```

To check reload stability, retain the first snapshot as a separate JSON file,
reload, export again and pass both paths:

```powershell
node tools/gt_addons/verify_snapshot.cjs after.json before.json
```

The checker reads actual recipes from the server registry, including generated
research recipes. It checks item resolution, voltages, computation, research NBT,
cross-mod inputs, duplicate replacement and canonical snapshot equality.
The recorded result is `validation-20261112.json`.

The quest description patch is separately stored in
`pack/quest_patches/gt_addon_descriptions.patch`. Its four target chapters are
already edited in the local pack. Do not apply it there a second time.

The independent GTNN/GTCA progression chapters, layout results and space-module
recipe validation are documented in docs/GT_ADDON_CHAPTERS_20261112.md.
verify_quests.py exports the live chapter item IDs and can compare them
against the server probe. verify_space_snapshot.cjs checks the module,
drone, motor, structure, orbital batch and mining recipe contracts.
