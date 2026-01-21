(function() {
  return [
    ['true', bhTextDb.get('node', 'mutex-block-wait-opts', 'true')],
    ['false', bhTextDb.get('node', 'mutex-block-wait-opts', 'false')]
  ];
})();
