(function() {
  return [
    ['true', bhTextDb.get('node', 'semaphore-wait-opts', 'true')],
    ['false', bhTextDb.get('node', 'semaphore-wait-opts', 'false')]
  ];
})();
