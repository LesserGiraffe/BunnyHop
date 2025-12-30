(function() {
  return [
    ['true', bhTextDb.get('node', 'semaphore-wait-ops', 'true')],
    ['false', bhTextDb.get('node', 'semaphore-wait-ops', 'false')]
  ];
})();
