(function() {
  return [
    ['withCountdown', bhTextDb.get('node', 'sync-timer-countdown-opts', 'true')],
    ['withoutCountdown', bhTextDb.get('node', 'sync-timer-countdown-opts', 'false')]
  ];
})();
