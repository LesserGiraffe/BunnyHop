(function() {
  return [
    ['floor', bhTextDb.get('node', 'num-round-opts', 'floor')],
    ['ceil',  bhTextDb.get('node', 'num-round-opts', 'ceil')],
    ['round', bhTextDb.get('node', 'num-round-opts', 'round')],
    ['trunc', bhTextDb.get('node', 'num-round-opts', 'trunc')]
  ];
})();
