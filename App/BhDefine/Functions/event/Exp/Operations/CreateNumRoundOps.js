(function() {
  return [
    ['floor', bhTextDb.get('node', 'num-round-ops', 'floor')],
    ['ceil',  bhTextDb.get('node', 'num-round-ops', 'ceil')],
    ['round', bhTextDb.get('node', 'num-round-ops', 'round')],
    ['trunc', bhTextDb.get('node', 'num-round-ops', 'trunc')]
  ];
})();
