(function() {
  return [
    ['withCountdown', bhTextDb.get('node', 'sync-timer-countdown-ops', 'true')],
    ['withoutCountdown', bhTextDb.get('node', 'sync-timer-countdown-ops', 'false')]
  ];
})();
