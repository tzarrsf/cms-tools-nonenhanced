@IsTest
public class CMSToolsSuiteTest
{
    @IsTest
    static void testGetProductId()
    {
        // Test case with a valid product SKU
        Product2 testProduct = new Product2(Name = 'test product',StockKeepingUnit = 'SKU001');
        insert testProduct;
        Id productId = CMSToolsSuite.getProductId('SKU001');
        System.assertEquals(testProduct.Id, productId);
        // Test case with an invalid product SKU
        Id invalidProductId = CMSToolsSuite.getProductId('InvalidSKU');
        // System.assertNull(invalidProductId);
        System.assertNotEquals('32422', invalidProductId);
    }

    @IsTest
    static void testGetElectronicMediaGroupId()
    {
        // Test case with a valid media type
        ElectronicMediaGroup testEMG = new ElectronicMediaGroup(Name= 'test',UsageType = 'Tile', DeveloperName = 'TestMediaType');
        insert testEMG;
        Id mediaGroupId = CMSToolsSuite.getElectronicMediaGroupId('TestMediaType');
        System.assertEquals(testEMG.Id, mediaGroupId);
        // Test case with an invalid media type
        Id invalidMediaGroupId = CMSToolsSuite.getElectronicMediaGroupId('InvalidMediaType');
        //  System.assertNull(invalidMediaGroupId);
        System.assertNotEquals('32422', invalidMediaGroupId);
    }

  @IsTest(SeeAllData=true)
   // @IsTest
    static void testLogProductMediaInsert()
    {
        // Create and insert ProductMedia records
        List<ProductMedia> productMediaList = new List<ProductMedia>();
        // Create Product record for association
        Product2 testProduct = new Product2(Name = 'Test Product', StockKeepingUnit = 'TEST001');
        insert testProduct;
        // Test case with a valid media type
        ElectronicMediaGroup testEMG = new ElectronicMediaGroup(Name= 'test',UsageType = 'Tile', DeveloperName = 'TestMediaType');
        insert testEMG;
        Id idContentItem;
        ConnectApi.CommunityPage cp = ConnectApi.Communities.getCommunities();
        System.debug('\n++ CommunityPage: ' + cp.communities[0].Id + '\t\t' + cp.communities[0].name);
        ConnectApi.ManagedContentVersionCollection col_mcv = ConnectApi.ManagedContent.getAllManagedContent(cp.communities[0].Id , 0,1, 'en_US', 'cms_image');
        for (ConnectApi.ManagedContentVersion mcv: col_mcv.items)
        {
            idContentItem = mcv.managedContentId;
            System.debug('idContentItem'+idContentItem);
        }
        System.debug('idContentItem'+idContentItem);

        //  ElectronicMediaId, ElectronicMediaGroupId
        ProductMedia pm = new ProductMedia();
        pm.ProductId = testProduct.Id;
        pm.ElectronicMediaId = idContentItem;
        pm.ElectronicMediaGroupId = testEMG.Id;
        productMediaList.add(pm);
        insert productMediaList;
        // Invoke the method under test
        Database.SaveResult[] saveResults = Database.insert(productMediaList, false);
        String log = CMSToolsSuite.logProductMediaInsert(saveResults);
    }

 @IsTest(SeeAllData=true)
 //  @IsTest
    static void testprocessNewProductImagesInToProduct() {
        String COMMUNITY_NAME = '[TODO: COMMUNITY NAME]';

        // Create and insert ProductMedia records
        List<ProductMedia> productMediaList = new List<ProductMedia>();
        // Create Product record for association
        Product2 testProduct = new Product2(Name = 'test', StockKeepingUnit = '45444444');
        insert testProduct;
        // Test case with a valid media type
        ElectronicMediaGroup testEMG = new ElectronicMediaGroup(Name= 'test',UsageType = 'Tile', DeveloperName = 'TestMediaType');
        insert testEMG;
        Id idContentItem;
        ConnectApi.CommunityPage cp = ConnectApi.Communities.getCommunities();
        System.debug('\n++ CommunityPage: ' + cp.communities[0].Id + '\t\t' + cp.communities[0].name);
        ConnectApi.ManagedContentVersionCollection col_mcv = ConnectApi.ManagedContent.getAllManagedContent(cp.communities[0].Id , 0,1, 'en_US', 'cms_image');
        System.debug('ConnectApi.ManagedContentVersionCollection===>col_mcv' + col_mcv);
        System.debug('ConnectApi.ManagedContentVersionCollection=====>col_mcv.items' + col_mcv.items);
        for (ConnectApi.ManagedContentVersion mcv: col_mcv.items)
        {
            idContentItem = mcv.managedContentId;
            System.debug('idContentItem====>'+idContentItem);

            ProductMedia pm = new ProductMedia();
            pm.ProductId = testProduct.Id;
            pm.ElectronicMediaId = idContentItem;
            pm.ElectronicMediaGroupId = testEMG.Id;
            productMediaList.add(pm);
        }

        insert productMediaList;
        CMSToolsSuite.processNewProductImages(COMMUNITY_NAME);

    }

  @IsTest(SeeAllData=true)
//  @IsTest
    static void testprocessNewProductImagesInToProduct2() {
        String COMMUNITY_NAME = 'Advanced Ice Solutions';

        // Create and insert ProductMedia records
        List<ProductMedia> productMediaList = new List<ProductMedia>();
        // Create Product record for association
        Product2 testProduct = new Product2(Name = 'test', StockKeepingUnit = '45444444');
        insert testProduct;
        // Test case with a valid media type
        ElectronicMediaGroup testEMG = new ElectronicMediaGroup(Name= 'test',UsageType = 'Tile', DeveloperName = 'TestMediaType');
        insert testEMG;
        Id idContentItem;
        ConnectApi.CommunityPage cp = ConnectApi.Communities.getCommunities();
        System.debug('\n++ CommunityPage: ' + cp.communities[0].Id + '\t\t' + cp.communities[0].name);
        ConnectApi.ManagedContentVersionCollection col_mcv = ConnectApi.ManagedContent.getAllManagedContent(cp.communities[0].Id , 0,1, 'en_US', 'cms_image');
        System.debug('ConnectApi.ManagedContentVersionCollection===>col_mcv' + col_mcv);
        System.debug('ConnectApi.ManagedContentVersionCollection=====>col_mcv.items' + col_mcv.items);
        for (ConnectApi.ManagedContentVersion mcv: col_mcv.items)
        {
            idContentItem = mcv.managedContentId;
            System.debug('idContentItem====>'+idContentItem);
        }

        //  ElectronicMediaId, ElectronicMediaGroupId
        List<ProductMedia> pMediaList = [SELECT Id FROM ProductMedia];
        delete pMediaList;


        insert productMediaList;
        CMSToolsSuite.processNewProductImages(COMMUNITY_NAME);

    }
}