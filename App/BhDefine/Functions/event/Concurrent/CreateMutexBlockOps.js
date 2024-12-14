(function() {
    return [
      ['true', bhTextDb.get('node', 'mutex-block-ops', 'true')],
      ['false', bhTextDb.get('node', 'mutex-block-ops', 'false')]
    ];
})();
