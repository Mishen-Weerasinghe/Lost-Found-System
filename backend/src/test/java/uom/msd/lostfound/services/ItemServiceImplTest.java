package uom.msd.lostfound.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uom.msd.lostfound.dto.ItemRequestDTO;
import uom.msd.lostfound.dto.ItemResponseDTO;
import uom.msd.lostfound.enums.ItemStatus;
import uom.msd.lostfound.enums.ReportType;
import uom.msd.lostfound.models.Item;
import uom.msd.lostfound.models.ItemImage;
import uom.msd.lostfound.models.User;
import uom.msd.lostfound.repositories.ItemRepository;
import uom.msd.lostfound.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Unit Tests")
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User testUser;
    private Item testItem;
    private ItemRequestDTO itemRequestDTO;
    private ItemResponseDTO itemResponseDTO;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testUser = mock(User.class);
        lenient().when(testUser.getId()).thenReturn(1L);

        testItem = new Item();
        testItem.setId(1L);
        testItem.setTitle("Lost Car Keys");
        testItem.setDescription("Silver car keys with keychain");
        testItem.setCategory("Electronics");
        testItem.setLocation("Library");
        testItem.setReportType(ReportType.LOST);
        testItem.setStatus(ItemStatus.OPEN);
        testItem.setUser(testUser);
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());
        testItem.setImages(new ArrayList<>());

        itemRequestDTO = new ItemRequestDTO(
                "Lost Car Keys",
                "Silver car keys with keychain",
                "Electronics",
                "Library",
                ReportType.LOST
        );
    }

    // ==================== Create Item Tests ====================

    @Test
    @DisplayName("Should create item successfully")
    void testCreateItem_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemResponseDTO result = itemService.createItem(1L, itemRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Lost Car Keys", result.getTitle());
        assertEquals(ReportType.LOST, result.getReportType());
        assertEquals(ItemStatus.OPEN, result.getStatus());
        assertEquals(1L, result.getUserId());

        // Verify interactions
        verify(userRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when user not found during creation")
    void testCreateItem_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> itemService.createItem(999L, itemRequestDTO));
        verify(userRepository, times(1)).findById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create item with images")
    void testCreateItem_WithImages() {
        // Arrange
        itemRequestDTO.setImageUrls(Arrays.asList(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
        ));

        ItemImage image1 = new ItemImage(testItem, "https://example.com/image1.jpg");
        ItemImage image2 = new ItemImage(testItem, "https://example.com/image2.jpg");
        testItem.addImage(image1);
        testItem.addImage(image2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemResponseDTO result = itemService.createItem(1L, itemRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getImageUrls().size());
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    // ==================== Get Item Tests ====================

    @Test
    @DisplayName("Should get item by ID successfully")
    void testGetItemById_Success() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // Act
        ItemResponseDTO result = itemService.getItemById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Lost Car Keys", result.getTitle());
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw RuntimeException when item not found")
    void testGetItemById_NotFound() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> itemService.getItemById(999L));
        verify(itemRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get all items")
    void testGetAllItems() {
        // Arrange
        Item item2 = new Item();
        item2.setId(2L);
        item2.setTitle("Found Phone");
        item2.setReportType(ReportType.FOUND);
        item2.setStatus(ItemStatus.OPEN);
        item2.setUser(testUser);
        item2.setCreatedAt(LocalDateTime.now());
        item2.setUpdatedAt(LocalDateTime.now());
        item2.setImages(new ArrayList<>());

        List<Item> items = Arrays.asList(testItem, item2);
        when(itemRepository.findAll()).thenReturn(items);

        // Act
        List<ItemResponseDTO> results = itemService.getAllItems();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Lost Car Keys", results.get(0).getTitle());
        assertEquals("Found Phone", results.get(1).getTitle());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no items exist")
    void testGetAllItems_Empty() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ItemResponseDTO> results = itemService.getAllItems();

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(itemRepository, times(1)).findAll();
    }

    // ==================== Get by Type Tests ====================

    @Test
    @DisplayName("Should get items by LOST type")
    void testGetItemsByType_Lost() {
        // Arrange
        List<Item> lostItems = Arrays.asList(testItem);
        when(itemRepository.findByReportType(ReportType.LOST)).thenReturn(lostItems);

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByType(ReportType.LOST);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(ReportType.LOST, results.get(0).getReportType());
        verify(itemRepository, times(1)).findByReportType(ReportType.LOST);
    }

    @Test
    @DisplayName("Should get items by FOUND type")
    void testGetItemsByType_Found() {
        // Arrange
        Item foundItem = new Item();
        foundItem.setId(2L);
        foundItem.setTitle("Found Phone");
        foundItem.setReportType(ReportType.FOUND);
        foundItem.setStatus(ItemStatus.OPEN);
        foundItem.setUser(testUser);
        foundItem.setCreatedAt(LocalDateTime.now());
        foundItem.setUpdatedAt(LocalDateTime.now());
        foundItem.setImages(new ArrayList<>());

        when(itemRepository.findByReportType(ReportType.FOUND)).thenReturn(Arrays.asList(foundItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByType(ReportType.FOUND);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ReportType.FOUND, results.get(0).getReportType());
    }

    // ==================== Get by Status Tests ====================

    @Test
    @DisplayName("Should get items by OPEN status")
    void testGetItemsByStatus_Open() {
        // Arrange
        when(itemRepository.findByStatus(ItemStatus.OPEN)).thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByStatus(ItemStatus.OPEN);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ItemStatus.OPEN, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get items by CLAIMED status")
    void testGetItemsByStatus_Claimed() {
        // Arrange
        testItem.setStatus(ItemStatus.CLAIMED);
        when(itemRepository.findByStatus(ItemStatus.CLAIMED)).thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByStatus(ItemStatus.CLAIMED);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ItemStatus.CLAIMED, results.get(0).getStatus());
    }

    // ==================== Get by User Tests ====================

    @Test
    @DisplayName("Should get user's items")
    void testGetUserItems() {
        // Arrange
        when(itemRepository.findByUserId(1L)).thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getUserItems(1L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getUserId());
        verify(itemRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Should return empty list when user has no items")
    void testGetUserItems_NoItems() {
        // Arrange
        when(itemRepository.findByUserId(999L)).thenReturn(new ArrayList<>());

        // Act
        List<ItemResponseDTO> results = itemService.getUserItems(999L);

        // Assert
        assertTrue(results.isEmpty());
    }

    // ==================== Filtering Tests ====================

    @Test
    @DisplayName("Should get items by type and status")
    void testGetItemsByTypeAndStatus() {
        // Arrange
        when(itemRepository.findByReportTypeAndStatus(ReportType.LOST, ItemStatus.OPEN))
                .thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByTypeAndStatus(ReportType.LOST, ItemStatus.OPEN);

        // Assert
        assertEquals(1, results.size());
        assertEquals(ReportType.LOST, results.get(0).getReportType());
        assertEquals(ItemStatus.OPEN, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get items by category")
    void testGetItemsByCategory() {
        // Arrange
        when(itemRepository.findBysCategoryAndType("Electronics", ReportType.LOST))
                .thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByCategory("Electronics", ReportType.LOST);

        // Assert
        assertEquals(1, results.size());
        assertEquals("Electronics", results.get(0).getCategory());
    }

    @Test
    @DisplayName("Should get items by location and status")
    void testGetItemsByLocation() {
        // Arrange
        when(itemRepository.findByLocationAndStatus("Library", ItemStatus.OPEN))
                .thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.getItemsByLocation("Library", ItemStatus.OPEN);

        // Assert
        assertEquals(1, results.size());
        assertEquals("Library", results.get(0).getLocation());
    }

    // ==================== Search Tests ====================

    @Test
    @DisplayName("Should search items by term")
    void testSearchItems() {
        // Arrange
        when(itemRepository.searchByTitleOrDescription("keys")).thenReturn(Arrays.asList(testItem));

        // Act
        List<ItemResponseDTO> results = itemService.searchItems("keys");

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getTitle().toLowerCase().contains("keys") ||
                   results.get(0).getDescription().toLowerCase().contains("keys"));
    }

    @Test
    @DisplayName("Should return empty list for search with no matches")
    void testSearchItems_NoMatches() {
        // Arrange
        when(itemRepository.searchByTitleOrDescription("nonexistent")).thenReturn(new ArrayList<>());

        // Act
        List<ItemResponseDTO> results = itemService.searchItems("nonexistent");

        // Assert
        assertTrue(results.isEmpty());
    }

    // ==================== Update Tests ====================

    @Test
    @DisplayName("Should update item status successfully")
    void testUpdateItemStatus_Success() {
        // Arrange
        testItem.setStatus(ItemStatus.OPEN);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemResponseDTO result = itemService.updateItemStatus(1L, ItemStatus.CLAIMED);

        // Assert
        assertNotNull(result);
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when updating status of non-existent item")
    void testUpdateItemStatus_ItemNotFound() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> itemService.updateItemStatus(999L, ItemStatus.CLAIMED));
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update item status to RESOLVED")
    void testUpdateItemStatus_ToResolved() {
        // Arrange
        testItem.setStatus(ItemStatus.CLAIMED);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        itemService.updateItemStatus(1L, ItemStatus.CLOSED);

        // Assert
        verify(itemRepository).save(argThat(item -> item.getStatus() == ItemStatus.CLOSED));
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should delete item successfully")
    void testDeleteItem_Success() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        doNothing().when(itemRepository).delete(any(Item.class));

        // Act
        itemService.deleteItem(1L);

        // Assert
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("Should throw RuntimeException when deleting non-existent item")
    void testDeleteItem_ItemNotFound() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> itemService.deleteItem(999L));
        verify(itemRepository, never()).delete(any());
    }

    // ==================== Image Tests ====================

    @Test
    @DisplayName("Should add image to item successfully")
    void testAddImageToItem_Success() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemResponseDTO result = itemService.addImageToItem(1L, "https://example.com/image.jpg");

        // Assert
        assertNotNull(result);
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("Should throw RuntimeException when item not found for image add")
    void testAddImageToItem_ItemNotFound() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> itemService.addImageToItem(999L, "https://example.com/image.jpg"));
        verify(itemRepository, never()).save(any());
    }

    // ==================== Pagination Tests ====================

    @Test
    @DisplayName("Should get items with pagination")
    void testGetItemsWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Item> items = Arrays.asList(testItem);
        PageImpl<Item> page = new PageImpl<>(items, pageable, 1);

        when(itemRepository.findAll(pageable)).thenReturn(page);

        // Act
        List<ItemResponseDTO> results = itemService.getItemsWithPagination(0, 10);

        // Assert
        assertEquals(1, results.size());
        verify(itemRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get items with different page sizes")
    void testGetItemsWithPagination_DifferentSizes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);
        PageImpl<Item> page = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        List<ItemResponseDTO> results = itemService.getItemsWithPagination(0, 5);

        // Assert
        assertTrue(results.isEmpty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(pageableCaptor.capture());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Should throw exception when deleting a non-existent item")
    void testDeleteItem_NotFound() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> itemService.deleteItem(999L));
        verify(itemRepository, never()).delete(any(Item.class));
    }
}
