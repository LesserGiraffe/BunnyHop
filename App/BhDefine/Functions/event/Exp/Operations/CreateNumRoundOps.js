(function() {
  let jSelectableItem = net.seapanda.bunnyhop.view.node.part.SelectableItem;
  return [
    new jSelectableItem('floor', '切り捨て'),
    new jSelectableItem('ceil', '切り上げ'),
    new jSelectableItem('round', '四捨五入')];
})();
