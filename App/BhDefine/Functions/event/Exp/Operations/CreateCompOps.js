(function() {
  let jSelectableItem = net.seapanda.bunnyhop.view.node.part.SelectableItem;
  return [
    new jSelectableItem('eq', '＝'),
    new jSelectableItem('lt', '＜'),
    new jSelectableItem('lte', '≦'),
        new jSelectableItem('gt', '＞'),
        new jSelectableItem('gte', '≧'),
        new jSelectableItem('neq', '≠')];
})();
