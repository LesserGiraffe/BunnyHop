(function() {
  bhText = String(bhText);
  if (bhText.length > 512) {
    return false;
  }
  let decRegex = /^[1-9](_?\d)*([eE][-+]?(0|[1-9](_?\d)*))?$/;
  if (decRegex.test(bhText)) {
    bhText = bhText.replaceAll('_', '');
    return Number.isInteger(Number.parseFloat(bhText));
  }

  let hexRegex = /^0[xX][\dA-Fa-f](_?[\dA-Fa-f])*$/;
  if (hexRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0x|0X)/g, '');
    let num = Number.parseInt(bhText, 16);
    return Number.isInteger(num) && (num >= 1);
  }

  let binRegex = /^0[bB][01](_?[01])*$/;
  if (binRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0b|0B)/g, '');
    let num = Number.parseInt(bhText, 2);
    return Number.isInteger(num) && (num >= 1);
  }

  return false;
})();
