(function() {
  return [
    ['finite', bhTextDb.get('node', 'num-type', 'finite')],
    ['infinite', bhTextDb.get('node', 'num-type', 'infinite')],
    ['nan', bhTextDb.get('node', 'num-type', 'nan')]
  ];
})();
