(function() {
  return [
    ['finite', bhTextDb.get('node', 'num-type', 'finite')],
    ['posInf', bhTextDb.get('node', 'num-type', 'posInf')],
    ['negInf', bhTextDb.get('node', 'num-type', 'negInf')],
    ['infinite', bhTextDb.get('node', 'num-type', 'infinite')],
    ['nan', bhTextDb.get('node', 'num-type', 'nan')]
  ];
})();
