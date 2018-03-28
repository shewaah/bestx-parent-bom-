
CREATE TABLE [CustomerAttributes] ( 
    [Id]                          	int NOT NULL,
    [AddCommissionToCustomerPrice]	bit NOT NULL DEFAULT (0),
    [MagnetProposalOnInexecution] 	bit NULL,
    [InternalCustomer]            	bit NULL,
    [OnlyEUROrders]               	bit NULL,
    [AmountCommissionWanted]      	bit NULL DEFAULT ((0)),
    CONSTRAINT [PK__IdCustomerAttributes] PRIMARY KEY([Id])
)
GO

INSERT INTO [CustomerAttributes] SELECT * FROM [AkrosCustomerAttributes]
GO

ALTER TABLE [dbo].[CustomerTable]
    DROP CONSTRAINT [FK__CustomerAttributes]
GO

DROP TABLE [AkrosCustomerAttributes]
GO 

