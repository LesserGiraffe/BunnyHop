(function() {
  return [
    ['true', bhTextDb.get('node', 'mutex-block-wait-ops', 'true')],
    ['false', bhTextDb.get('node', 'mutex-block-wait-ops', 'false')]
  ];
})();
