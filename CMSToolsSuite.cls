public with sharing class CMSToolsSuite {
    public static Id getProductId(String productSku) {
        Id returnId = null;
        if (productSku != null) {
            List<Product2> products = [SELECT Id FROM Product2 WHERE StockKeepingUnit = :productSku LIMIT 1];
            if (!products.isEmpty()) {
                returnId = products[0].Id;
            }
        }
        return returnId;
    }


    public static Id getElectronicMediaGroupId(String mediaType) {
            Id returnId = null;
            List<ElectronicMediaGroup> eMG = [SELECT Id FROM ElectronicMediaGroup WHERE DeveloperName = :mediaType LIMIT 1];
            if (!emg.isEmpty()) {
                returnId = eMG[0].Id;
            }
            return returnId;
    }



    // assigns images to products where images isn't currently assigned
    public static void processNewProductImages(String communityName) {
        Map<String, ConnectApi.ManagedContentVersion> contentVersionToProductMap = new Map<String, ConnectApi.ManagedContentVersion>();
        Map<String, ConnectApi.ManagedContentVersion> imageMap = new Map<String, ConnectApi.ManagedContentVersion>();
        Map<String, String> assignedMedias = new Map<String, String>();

        Set<Id> productIdsHasMedia = new Set<Id>();
        List<ProductMedia> productMediaList = [SELECT Id, Name, Product.StockKeepingUnit, ElectronicMediaGroupId, ElectronicMediaId, ProductId FROM ProductMedia];
        for (ProductMedia productMediaItem : productMediaList) {
            productIdsHasMedia.add(productMediaItem.ProductId);
            assignedMedias.put ((String) productMediaItem.ElectronicMediaId, (String) productMediaItem.Product.StockKeepingUnit);
        }
        System.debug('++ productMediaList: ' + productMediaList.size());

        // https://salesforce.stackexchange.com/questions/96153/can-i-get-community-id-without-connectapi
        Network network = [SELECT Id FROM Network WHERE Name = :communityName LIMIT 1];
        ConnectApi.ManagedContentVersionCollection collection = ConnectApi.ManagedContent.getAllManagedContent(network.Id , 0,200, 'en_US', 'cms_image');

        String productSku = '';
        String sortOrder = '';
        String productSkuSortOrder = '';
        for (ConnectApi.ManagedContentVersion item : collection.items) {

// TODO: Remove debug, fails with old CMS conent data OR add try/catch
            if (Pattern.matches('(.*)-(productimage)-(.*)', item.contentUrlName)) {
                try {
                    productSkuSortOrder = item.contentUrlName.substringAfterLast('-productimage-');
                    productSku = productSkuSortOrder.substring(0, productSkuSortOrder.indexOf('-sku')).toUpperCase();
                    sortOrder = productSkuSortOrder.substringAfterLast('-order-');
                    // System.debug('++ ContentURL: ' + item.contentUrlName);
                    System.debug('++ productSku/Sort Order: ' + productSku + '\t' + sortOrder);
                } catch(Exception  e) {
                    productSku = '';
                }
            }


            if ( !String.isEmpty(productSku) && assignedMedias.get(item.managedContentId) == null ) {
                contentVersionToProductMap.put(productSku, item);
                if (Pattern.matches('(.*)-(productimage)-(.*)', item.contentUrlName)) {
                    imageMap.put(item.managedContentId, item);
                }
            }
        }
        System.debug('++ imageMap Size: ' + imageMap.size());


        Map<String, String> productSkuToProductIdMap = new Map<String, String>();
        List<Product2> productListHasMedia = [SELECT Id, StockKeepingUnit FROM Product2 WHERE StockKeepingUnit IN :contentVersionToProductMap.keySet()];
        for (Product2 product: productListHasMedia) {
            productIdsHasMedia.add(product.Id);
            productSkuToProductIdMap.put(product.StockKeepingUnit.toUpperCase(), product.Id);
        }
        System.debug('++ productSkuToProductIdMap Size: ' + productSkuToProductIdMap.size());

        String productListImageGroupId = getElectronicMediaGroupId('productListImage');
        String productDetailImageGroupId = getElectronicMediaGroupId('productDetailImage');
        String producImageGroupId = productDetailImageGroupId;

        List<ProductMedia> productMediaListToInsert = new List<ProductMedia>();
        if (imageMap.size() > 0) {
            for(String imageMapKey: imageMap.keySet()) {
                ConnectApi.ManagedContentVersion contentVersion = imageMap.get(imageMapKey);
                Integer sequenceInt = 0;
                if (!String.isEmpty (contentVersion.contentUrlName)) {

                    productSkuSortOrder = contentVersion.contentUrlName.substringAfterLast('-productimage-');
                    productSku = productSkuSortOrder.substring(0, productSkuSortOrder.indexOf('-sku')).toUpperCase();
                    sortOrder = productSkuSortOrder.substringAfterLast('-order-');
                    sequenceInt = Integer.valueOf(sortOrder);
                    producImageGroupId = productDetailImageGroupId;
                    if (sequenceInt == 0) {
                        producImageGroupId = productListImageGroupId;
                    }

                    // System.debug('++ productSku: ' + productSku);
                    // System.debug('++ Sort Order: ' + sortOrder);
                    // System.debug('++ ProductId: ' + productSkuToProductIdMap.get(productSku));

                    productMediaListToInsert.add(new ProductMedia(
                        ElectronicMediaGroupId = producImageGroupId, ElectronicMediaId = contentVersion.managedContentId,
                        ProductId = productSkuToProductIdMap.get(productSku), Sortorder = sequenceInt
                    ));
                }
            }
        }
        System.debug('++ productMediaListToInsert Size: ' + productMediaListToInsert.size());


        Database.DMLOptions dmlo = new Database.DMLOptions();
        dmlo.OptAllOrNone = true;
        String log = '\nprocessNewProductImage Results ' + DateTime.now();

        if (productMediaListToInsert.size() > 0) {
                Database.SaveResult[] srlist = database.insert(productMediaListToInsert, dmlo);
                log = '\n' + logProductMediaInsert(srlist);
        }
        System.debug('\n' + log);
        System.debug('\n\n\n');
    }



    public static String logProductMediaInsert(Database.SaveResult[] srlist) {
        List<Id> productMediaIdList = new List<Id>();
        for (Database.SaveResult sr : srList) {
                if (sr.isSuccess()) {
                    productMediaIdList.add(sr.getId());
                } else {
                    for(Database.Error err : sr.getErrors()) {
                        System.debug('The following error has occurred.\n' + err.getStatusCode() + ': ' + err.getMessage() + '\n' + 'ProductMedia fields that affected this error: ' + err.getFields());
                    }
                }
            }

            List<ProductMedia> pmInsertResults  = [SELECT Id, ProductId, Product.Name, Product.StockKeepingUnit FROM ProductMedia WHERE Id IN :productMediaIdList ORDER BY Product.Name];
            String log = '\n\n#\tProductMedia Id\tSKU\tName\tProduct Id';
            Decimal i = 1;
            for (ProductMedia pm : pmInsertResults) {
                log = log + '\n' + i++ + '\t' + pm.Id + '\t' + pm.Product.StockKeepingUnit + '\t' + pm.Product.Name + '\t' + pm.ProductId;
            }
            return log;
    }

}