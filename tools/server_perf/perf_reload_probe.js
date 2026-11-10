ServerEvents.recipes(function (event) {
  var settings = JsonIO.read('kubejs/config/sunlit_perf_probe.json');
  var variant = settings ? String(settings.variant) : 'base';
  if (variant === 'removed') return;
  event.smelting('minecraft:stone', variant === 'alternate' ? 'minecraft:dirt' : 'minecraft:cobblestone')
    .id('sunlitindustry:perf_reload_probe');
});
