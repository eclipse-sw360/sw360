function dynamicSort(property, type) {
    var sortOrder = 1;

    if(property[0] === "-") {
        sortOrder = -1;

        property = property.substr(1);
    }

    return function (a,b) {
        var result;

        switch (type) {
            case 'int':
                result = (parseInt(a[property]) < parseInt(b[property])) ? -1 : (parseInt(a[property]) > (b[property])) ? 1 : 0;
                break;
            case 'string':
            default:
                result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
        }

        return  result * sortOrder;
    }
}