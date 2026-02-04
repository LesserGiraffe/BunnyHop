(function() {
  return [
    ['subset', bhTextDb.get('node', 'set-comp-opts', 'subset')],
    ['properSubset', bhTextDb.get('node', 'set-comp-opts', 'properSubset')],
    ['superset', bhTextDb.get('node', 'set-comp-opts', 'superset')],
    ['properSuperset', bhTextDb.get('node', 'set-comp-opts', 'properSuperset')],
    ['eq', bhTextDb.get('node', 'set-comp-opts', 'eq')],
    ['neq', bhTextDb.get('node', 'set-comp-opts', 'neq')]
  ];
})();
